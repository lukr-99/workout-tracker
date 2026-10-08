package com.lukr99.workout.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.transfer.SessionFingerprint
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.creation.WorkoutFactory
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.RunSource
import java.time.Duration
import java.time.Instant

class HealthConnectService internal constructor(
    private val repository: WorkoutRepository,
    private val gateway: HealthConnectGateway,
    private val factory: WorkoutFactory = WorkoutFactory(),
) {
    val requiredPermissions: Set<String> get() = gateway.requiredPermissions

    suspend fun availability(): HealthConnectAvailability = gateway.availability()

    suspend fun hasPermissions(): Boolean =
        availability() == HealthConnectAvailability.Available &&
            gateway.grantedPermissions().containsAll(requiredPermissions)

    suspend fun exportCompletedSessions(): HealthConnectSyncSummary {
        if (availability() != HealthConnectAvailability.Available) {
            return HealthConnectSyncSummary(unsupported = 1)
        }
        if (!hasPermissions()) return HealthConnectSyncSummary(skipped = 1)

        val completed = repository.getSessions()
            .filter { it.status == WorkoutSessionStatus.Completed }
        val exportable = completed.filter { it.source != WorkoutSessionSource.HealthConnect }
        val skipped = completed.size - exportable.size
        gateway.writeExerciseSessions(exportable.map(HealthConnectMapper::toHealthRecord))
        return HealthConnectSyncSummary(exported = exportable.size, skipped = skipped)
    }

    /**
     * Send one just-finished workout, for the automatic send. Best-effort like [exportRuns]: no
     * permission or a failed write is reported as skipped and never fails the saved workout.
     * Idempotent, since Health Connect upserts on the workout's fingerprint.
     */
    suspend fun exportWorkout(session: WorkoutSession): HealthConnectSyncSummary {
        if (availability() != HealthConnectAvailability.Available) return HealthConnectSyncSummary(unsupported = 1)
        if (!hasPermissions() || session.source == WorkoutSessionSource.HealthConnect) return HealthConnectSyncSummary(skipped = 1)
        return runCatching {
            gateway.writeExerciseSessions(listOf(HealthConnectMapper.toHealthRecord(session)))
            HealthConnectSyncSummary(exported = 1)
        }.getOrElse { HealthConnectSyncSummary(skipped = 1) }
    }

    /**
     * Write runs to Health Connect as Running [ExerciseSessionRecord]s with an ExerciseRoute +
     * distance/energy (R2). Idempotent by the run's stable id (Health Connect upserts on
     * `clientRecordId`), so re-exporting the same run updates rather than duplicates. Runs imported
     * *from* Health Connect are skipped to avoid an echo.
     */
    suspend fun exportRuns(runs: List<Run>): HealthConnectSyncSummary {
        if (availability() != HealthConnectAvailability.Available) {
            return HealthConnectSyncSummary(unsupported = 1)
        }
        if (!hasPermissions()) return HealthConnectSyncSummary(skipped = 1)

        val exportable = runs.filter { it.source != RunSource.HealthConnect }
        if (exportable.isEmpty()) return HealthConnectSyncSummary(skipped = runs.size)
        // Best-effort: distance/energy/route need their own grants; a missing one must never fail a
        // saved run. If the write throws (e.g. route permission not granted) report it as skipped.
        return runCatching {
            gateway.writeExerciseSessions(exportable.map(HealthConnectMapper::runToHealthRecord))
            HealthConnectSyncSummary(exported = exportable.size, skipped = runs.size - exportable.size)
        }.getOrElse { HealthConnectSyncSummary(skipped = runs.size) }
    }

    suspend fun importSessions(
        fromUtcMillis: Long = Instant.now().minus(Duration.ofDays(30)).toEpochMilli(),
        toUtcMillis: Long = System.currentTimeMillis(),
    ): HealthConnectSyncSummary {
        if (availability() != HealthConnectAvailability.Available) {
            return HealthConnectSyncSummary(unsupported = 1)
        }
        if (!hasPermissions()) return HealthConnectSyncSummary(skipped = 1)

        val records = gateway.readExerciseSessions(fromUtcMillis, toUtcMillis)
        val existingSessions = repository.getSessions(includeDiscarded = true)
        val externalKeys = existingSessions.mapNotNullTo(mutableSetOf(), WorkoutSession::externalKey)
        val fingerprints = existingSessions.mapTo(mutableSetOf(), SessionFingerprint::of)
        val catalog = repository.getExercises(ExerciseFilter(includeArchived = true))
            .associateBy(Exercise::id)
            .toMutableMap()
        var imported = 0
        var skipped = 0

        for (record in records) {
            val exportedFingerprint = record.clientRecordId
                ?.takeIf { it.startsWith(ClientRecordPrefix) }
                ?.removePrefix(ClientRecordPrefix)
            val providerKey = record.clientRecordId ?: record.recordId
            val externalKey = "health-connect:${record.dataOriginPackageName}:$providerKey"
            if (externalKey in externalKeys || exportedFingerprint?.let(fingerprints::contains) == true) {
                skipped++
                continue
            }

            val exerciseDraft = HealthConnectMapper.exerciseDraft(record)
            val exercise = catalog[exerciseDraft.id] ?: factory.exercise(exerciseDraft).requireValid()
                .also {
                    repository.saveExercise(it)
                    catalog[it.id] = it
                }
            val session = factory.session(
                HealthConnectMapper.sessionDraft(record, exercise, externalKey),
                catalog,
            ).requireValid()
            repository.saveWorkoutSession(session)
            externalKeys += externalKey
            fingerprints += SessionFingerprint.of(session)
            imported++
        }
        return HealthConnectSyncSummary(imported = imported, skipped = skipped)
    }

    companion object {
        internal const val ClientRecordPrefix = "workout-tracker:"
        internal const val ClientRunPrefix = "workout-tracker:run:"
    }
}
