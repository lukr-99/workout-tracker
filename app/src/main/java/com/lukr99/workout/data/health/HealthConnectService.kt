package com.lukr99.workout.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.transfer.SessionFingerprint
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.creation.CardioDraft
import com.lukr99.workout.domain.creation.EntryDraft
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.domain.creation.SessionDraft
import com.lukr99.workout.domain.creation.WorkoutFactory
import java.security.MessageDigest
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
    }
}

data class HealthConnectSyncSummary(
    val imported: Int = 0,
    val exported: Int = 0,
    val skipped: Int = 0,
    val unsupported: Int = 0,
)

internal object HealthConnectMapper {
    fun toHealthRecord(session: WorkoutSession): HealthWorkoutRecord {
        val end = session.endedAtUtc
            ?: (session.startedAtUtc + session.durationSeconds.coerceAtLeast(1) * 1_000)
        return HealthWorkoutRecord(
            clientRecordId = HealthConnectService.ClientRecordPrefix + SessionFingerprint.of(session),
            title = session.name,
            notes = session.notes,
            startTimeUtcMillis = session.startedAtUtc,
            endTimeUtcMillis = end.coerceAtLeast(session.startedAtUtc + 1),
            exerciseType = typeFor(session),
            bodyweightKg = session.bodyweightKg,
        )
    }

    fun exerciseDraft(record: HealthWorkoutRecord): ExerciseDraft {
        val (name, category, bodyPart) = exerciseDetails(record.exerciseType)
        return ExerciseDraft(
            id = stableId("health-exercise:${record.exerciseType}"),
            name = name,
            category = category,
            primaryBodyPart = bodyPart,
            source = ExerciseSource.Synced,
            externalSourceId = "health-connect:${record.exerciseType}",
        )
    }

    fun sessionDraft(
        record: HealthWorkoutRecord,
        exercise: Exercise,
        externalKey: String,
    ): SessionDraft {
        val durationSeconds =
            ((record.endTimeUtcMillis - record.startTimeUtcMillis) / 1_000).coerceAtLeast(1)
        val cardio = exercise.category == ExerciseCategory.Cardio
        return SessionDraft(
            id = stableId(externalKey),
            name = record.title.ifBlank { exercise.name },
            startedAtUtc = record.startTimeUtcMillis,
            endedAtUtc = record.endTimeUtcMillis,
            completedDateUtc = record.endTimeUtcMillis,
            durationSeconds = durationSeconds,
            notes = record.notes,
            status = WorkoutSessionStatus.Completed,
            source = WorkoutSessionSource.HealthConnect,
            externalKey = externalKey,
            bodyweightKg = record.bodyweightKg,
            entries = listOf(
                EntryDraft(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    category = exercise.category,
                    bodyPart = exercise.primaryBodyPart,
                    cardio = if (cardio) CardioDraft(durationSeconds = durationSeconds.toInt()) else null,
                ),
            ),
        )
    }

    private fun typeFor(session: WorkoutSession): Int {
        val entries = session.entries
        val names = entries.joinToString(" ") { it.exerciseSnapshotName.lowercase() }
        return when {
            "run" in names -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "walk" in names -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "bike" in names || "cycling" in names -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
            entries.any { it.entryType == ExerciseCategory.Strength } ->
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }

    private fun exerciseDetails(type: Int): Triple<String, ExerciseCategory, String> = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ->
            Triple("Running", ExerciseCategory.Cardio, "Cardio")
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING ->
            Triple("Walking", ExerciseCategory.Cardio, "Cardio")
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING ->
            Triple("Cycling", ExerciseCategory.Cardio, "Cardio")
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING ->
            Triple("Strength Training", ExerciseCategory.Strength, "Full Body")
        else -> Triple("Health Connect Workout", ExerciseCategory.Cardio, "Full Body")
    }

    private fun stableId(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .take(16)
            .joinToString("") { "%02x".format(it) }
}
