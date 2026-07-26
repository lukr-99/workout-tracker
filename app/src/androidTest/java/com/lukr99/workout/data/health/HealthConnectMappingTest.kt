package com.lukr99.workout.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectMappingTest {
    @Test
    fun cardioSessionMapsToExerciseSessionRecordShape() {
        val mapped = HealthConnectMapper.toHealthRecord(
            WorkoutSession(
                id = "cardio",
                name = "Evening cycling",
                startedAtUtc = 10_000,
                endedAtUtc = 70_000,
                status = WorkoutSessionStatus.Completed,
                entries = listOf(
                    WorkoutEntry(
                        exerciseSnapshotName = "Indoor bike",
                        entryType = ExerciseCategory.Cardio,
                    ),
                ),
            ),
        )

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, mapped.exerciseType)
        assertTrue(mapped.clientRecordId.orEmpty().startsWith("workout-tracker:"))
        assertEquals(60_000, mapped.endTimeUtcMillis - mapped.startTimeUtcMillis)
    }
}
