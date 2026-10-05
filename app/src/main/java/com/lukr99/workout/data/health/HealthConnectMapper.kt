package com.lukr99.workout.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.lukr99.workout.data.transfer.SessionFingerprint
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.creation.CardioDraft
import com.lukr99.workout.domain.creation.EntryDraft
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.domain.creation.SessionDraft
import com.lukr99.workout.domain.run.Run
import java.security.MessageDigest

internal object HealthConnectMapper {
    /** Map a run to a Running exercise record with its route + distance/energy. */
    fun runToHealthRecord(run: Run): HealthWorkoutRecord {
        val end = (run.startedAtUtc + run.durationSeconds.coerceAtLeast(1) * 1_000)
            .coerceAtLeast(run.startedAtUtc + 1)
        return HealthWorkoutRecord(
            clientRecordId = HealthConnectService.ClientRunPrefix + run.id,
            title = run.notes.ifBlank { "Run" },
            notes = run.notes,
            startTimeUtcMillis = run.startedAtUtc,
            endTimeUtcMillis = end,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            distanceMeters = run.distanceMeters.takeIf { it > 0 },
            totalEnergyKcal = run.calories?.takeIf { it > 0 },
            route = run.trace.map {
                HealthRoutePoint(
                    timeUtcMillis = run.startedAtUtc + it.t,
                    lat = it.lat,
                    lon = it.lon,
                    altitudeM = it.elevationM,
                )
            },
        )
    }

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
