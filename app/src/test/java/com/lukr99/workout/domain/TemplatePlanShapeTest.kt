package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplatePlanShapeTest {

    private fun ex(sets: Int? = 3, rest: Int? = null, group: Int? = null, category: ExerciseCategory = ExerciseCategory.Strength) =
        WorkoutTemplateExercise(targetSets = sets, restSeconds = rest, supersetGroup = group, category = category)

    @Test
    fun aSupersetSharesOneNumberWithLetters() {
        val plan = listOf(ex(), ex(), ex(), ex(group = 1), ex(group = 1), ex())

        assertEquals(listOf("1", "2", "3", "4a", "4b", "5"), TemplatePlanShape.labels(plan))
    }

    @Test
    fun twoSupersetsInARowAreTwoSteps() {
        val plan = listOf(ex(group = 1), ex(group = 1), ex(group = 1), ex(group = 2), ex(group = 2))

        assertEquals(listOf("1a", "1b", "1c", "2a", "2b"), TemplatePlanShape.labels(plan))
    }

    @Test
    fun durationCountsSetsRestAndChangeovers() {
        // 4 sets: 4 × 40 s work + 3 × 150 s rest + 120 s changeover = 730 s, about 12 min -> 10.
        assertEquals(10, TemplatePlanShape.estimatedMinutes(listOf(ex(sets = 4, rest = 150)), defaultRestSeconds = 120))
    }

    @Test
    fun aSupersetRestsOncePerRound() {
        val superset = listOf(ex(sets = 3, rest = 60, group = 1), ex(sets = 3, rest = 90, group = 1))
        val apart = listOf(ex(sets = 3, rest = 60), ex(sets = 3, rest = 90))
        // Together: 6 × 40 + 2 × 90 + 120 = 540 s (9 min -> 10). Apart: 120 + 120 + 120 + 120 + 180 + 120 = 780 s (13 -> 15).
        assertEquals(10, TemplatePlanShape.estimatedMinutes(superset, defaultRestSeconds = 120))
        assertEquals(15, TemplatePlanShape.estimatedMinutes(apart, defaultRestSeconds = 120))
    }

    @Test
    fun unplannedExercisesUseThreeSetsAndTheDefaultRest() {
        // 3 × 40 + 2 × 120 + 120 = 480 s, 8 min -> 10.
        assertEquals(10, TemplatePlanShape.estimatedMinutes(listOf(ex(sets = null)), defaultRestSeconds = 120))
        assertEquals(0, TemplatePlanShape.estimatedMinutes(emptyList(), defaultRestSeconds = 120))
    }

    @Test
    fun cardioCountsTenMinutes() {
        assertEquals(10, TemplatePlanShape.estimatedMinutes(listOf(ex(category = ExerciseCategory.Cardio)), defaultRestSeconds = 120))
    }
}
