package com.lukr99.workout.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectMapperTest {
    @Test
    fun completedStrengthSessionMapsToStableHealthRecord() {
        val session = WorkoutSession(
            id = "session",
            name = "Push",
            startedAtUtc = 1_000,
            endedAtUtc = 61_000,
            durationSeconds = 60,
            status = WorkoutSessionStatus.Completed,
            entries = listOf(
                WorkoutEntry(
                    exerciseSnapshotName = "Bench Press",
                    entryType = ExerciseCategory.Strength,
                    strengthSets = listOf(StrengthSet(reps = 5, weightKg = 100.0)),
                ),
            ),
        )

        val first = HealthConnectMapper.toHealthRecord(session)
        val second = HealthConnectMapper.toHealthRecord(session)

        assertEquals(first.clientRecordId, second.clientRecordId)
        assertTrue(first.clientRecordId!!.startsWith("workout-tracker:"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, first.exerciseType)
    }

    @Test
    fun importedRunningRecordProducesCompletedProvenancedDraft() {
        val record = HealthWorkoutRecord(
            recordId = "provider-id",
            title = "Morning run",
            startTimeUtcMillis = 1_000,
            endTimeUtcMillis = 3_601_000,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        )
        val exerciseDraft = HealthConnectMapper.exerciseDraft(record)
        val exercise = Exercise(
            id = exerciseDraft.id,
            name = exerciseDraft.name,
            category = exerciseDraft.category,
            primaryBodyPart = exerciseDraft.primaryBodyPart,
        )

        val draft = HealthConnectMapper.sessionDraft(
            record,
            exercise,
            "health-connect:provider:provider-id",
        )

        assertEquals(WorkoutSessionSource.HealthConnect, draft.source)
        assertEquals(WorkoutSessionStatus.Completed, draft.status)
        assertEquals("health-connect:provider:provider-id", draft.externalKey)
        assertEquals(3_600, draft.entries.single().cardio?.durationSeconds)
    }

    @Test
    fun runMapsToRunningRecordWithRouteAndDistanceIdempotently() {
        val run = com.lukr99.workout.domain.run.Run(
            id = "run-1",
            startedAtUtc = 10_000,
            durationSeconds = 300,
            movingSeconds = 300,
            distanceMeters = 1_000.0,
            calories = 80.0,
            notes = "Lunch loop",
            trace = listOf(
                com.lukr99.workout.domain.run.TracePoint(t = 0, lat = 50.0, lon = 14.0, elevationM = 200.0),
                com.lukr99.workout.domain.run.TracePoint(t = 300_000, lat = 50.009, lon = 14.0, elevationM = 210.0),
            ),
        )

        val first = HealthConnectMapper.runToHealthRecord(run)
        val second = HealthConnectMapper.runToHealthRecord(run)

        assertEquals(first.clientRecordId, second.clientRecordId) // idempotent
        assertTrue(first.clientRecordId!!.startsWith("workout-tracker:run:"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, first.exerciseType)
        assertEquals(1_000.0, first.distanceMeters!!, 0.001)
        assertEquals(80.0, first.totalEnergyKcal!!, 0.001)
        assertEquals(2, first.route.size)
        // Route point times are absolute (start + offset).
        assertEquals(10_000, first.route.first().timeUtcMillis)
        assertEquals(310_000, first.route.last().timeUtcMillis)
        assertEquals(310_000, first.endTimeUtcMillis)
    }
}
