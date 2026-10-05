package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseOutingsTest {

    private fun workout(at: Long, vararg sets: StrengthSet, status: WorkoutSessionStatus = WorkoutSessionStatus.Completed) =
        WorkoutSession(
            status = status,
            startedAtUtc = at,
            completedDateUtc = at,
            entries = listOf(WorkoutEntry(exerciseId = "bench", strengthSets = sets.toList())),
        )

    @Test
    fun outingsAreNewestFirstAndLimited() {
        val history = (1..5).map { workout(it * 1_000L, StrengthSet(reps = 5, weightKg = 80.0 + it)) }

        val outings = history.recentOutings("bench", limit = 3)

        assertEquals(listOf(5_000L, 4_000L, 3_000L), outings.map { it.atUtc })
    }

    @Test
    fun warmUpsEmptySetsAndLiveWorkoutsAreLeftOut() {
        val history = listOf(
            workout(1_000, StrengthSet(reps = 10, weightKg = 40.0, isWarmup = true), StrengthSet(reps = 0, weightKg = 80.0), StrengthSet(reps = 8, weightKg = 80.0)),
            workout(2_000, StrengthSet(reps = 8, weightKg = 85.0), status = WorkoutSessionStatus.Active),
        )

        val outings = history.recentOutings("bench")

        assertEquals(1, outings.size)
        assertEquals(listOf(8), outings.single().sets.map { it.reps })
        assertEquals(80.0 * (1 + 8 / 30.0), outings.single().bestE1rmKg, 0.001)
    }

    @Test
    fun aWorkoutWithoutTheExerciseIsSkipped() {
        assertTrue(listOf(workout(1_000)).recentOutings("bench").isEmpty())
        assertTrue(listOf(workout(1_000, StrengthSet(reps = 5))).recentOutings("").isEmpty())
    }

    @Test
    fun replacingKeepsTheSetsAndNotesAndTakesTheNewName() {
        val entry = WorkoutEntry(
            exerciseId = "bench",
            exerciseSnapshotName = "Bench press",
            exerciseSnapshotPrimaryBodyPart = "Chest",
            notes = "Felt strong",
            strengthSets = listOf(StrengthSet(id = "s1", reps = 8, weightKg = 30.0)),
        )
        val db = Exercise(id = "db", name = "Dumbbell press", primaryBodyPart = "Chest")

        val swapped = entry.replacedWith(db)

        assertEquals("db", swapped.exerciseId)
        assertEquals("Dumbbell press", swapped.exerciseSnapshotName)
        assertEquals("Felt strong", swapped.notes)
        assertEquals(listOf("s1"), swapped.strengthSets.map { it.id })
    }
}
