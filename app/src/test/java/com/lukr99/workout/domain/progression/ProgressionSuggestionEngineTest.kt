package com.lukr99.workout.domain.progression

import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionSuggestionEngineTest {
    @Test
    fun supportsDoubleLinearAndPercentageProgression() {
        val history = listOf(session("one", 1_000, 100.0, 12, 3))

        val double = ProgressionSuggestionEngine.suggest(
            history,
            ExerciseId,
            DoubleProgression(repRange = 8..12),
            DeloadPolicy(enabled = false),
        )
        assertEquals(TargetSet(8, 102.5), double.targets.first())

        val linear = ProgressionSuggestionEngine.suggest(
            listOf(session("one", 1_000, 100.0, 5, 3)),
            ExerciseId,
            LinearProgression(),
            DeloadPolicy(enabled = false),
        )
        assertEquals(List(3) { TargetSet(5, 102.5) }, linear.targets)

        val percentage = ProgressionSuggestionEngine.suggest(
            listOf(session("one", 1_000, 100.0, 5, 3)),
            ExerciseId,
            PercentOfEstimated1Rm(percentage = 0.75),
            DeloadPolicy(enabled = false),
        )
        assertEquals(87.5, percentage.targets.first().weightKg, 0.001)
    }

    @Test
    fun appliesLoadAndVolumeDeloadAfterConfiguredStall() {
        val history = (1L..4L).map { session("s$it", it * 1_000, 100.0, 5, 4) }
        val suggestion = ProgressionSuggestionEngine.suggest(
            history,
            ExerciseId,
            LinearProgression(targetSets = 4),
            DeloadPolicy(stallSessions = 3, loadReductionFraction = 0.1, volumeReductionFraction = 0.25),
        )

        assertTrue(suggestion.isDeload)
        assertEquals(3, suggestion.targets.size)
        assertEquals(92.0, suggestion.targets.first().weightKg, 0.001)
        assertFalse(suggestion.rationale.isBlank())
    }

    private fun session(
        id: String,
        date: Long,
        weight: Double,
        reps: Int,
        setCount: Int,
    ) = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = date,
        completedDateUtc = date,
        status = WorkoutSessionStatus.Completed,
        entries = listOf(
            WorkoutEntry(
                exerciseId = ExerciseId,
                exerciseSnapshotName = "Squat",
                strengthSets = List(setCount) { index ->
                    StrengthSet(setNumber = index + 1, weightKg = weight, reps = reps)
                },
            ),
        ),
    )

    private companion object {
        const val ExerciseId = "squat"
    }
}
