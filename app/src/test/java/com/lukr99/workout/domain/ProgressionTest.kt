package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L

    private fun session(name: String, at: Long, exerciseId: String, vararg sets: Pair<Int, Double>) =
        WorkoutSession(
            name = name,
            startedAtUtc = at,
            completedDateUtc = at,
            status = WorkoutSessionStatus.Completed,
            entries = listOf(
                WorkoutEntry(
                    exerciseId = exerciseId,
                    exerciseSnapshotName = exerciseId,
                    entryType = ExerciseCategory.Strength,
                    strengthSets = sets.mapIndexed { i, (reps, kg) ->
                        StrengthSet(setNumber = i + 1, reps = reps, weightKg = kg)
                    },
                ),
            ),
        )

    @Test
    fun forExercise_seriesIsChronologicalAndPerSession() {
        val sessions = listOf(
            session("late", now, "bench", 5 to 105.0, 5 to 105.0),
            session("early", now - 7 * day, "bench", 5 to 100.0),
            session("other", now - 3 * day, "squat", 5 to 140.0),
        )

        val points = Progression.forExercise(sessions, "bench")

        assertEquals(2, points.size)
        assertEquals(now - 7 * day, points[0].dateUtc) // sorted ascending
        assertEquals(100.0, points[0].bestWeightKg, 1e-9)
        assertEquals(105.0, points[1].bestWeightKg, 1e-9)
        assertEquals(105.0 * 5 * 2, points[1].totalVolumeKg, 1e-9)
        assertEquals(10, points[1].totalReps)
    }
}
