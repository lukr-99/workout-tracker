package com.lukr99.workout.data.importer

import com.lukr99.workout.data.transfer.ExerciseMatchMode
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportExerciseMatcherTest {

    private val bench = Exercise(id = "bench", name = "Barbell Bench Press")
    private val row = Exercise(id = "row", name = "Seated Cable Row")
    private val catalog = listOf(bench, row)

    private fun matcher(mode: ExerciseMatchMode) =
        ImportExerciseMatcher(catalog, ImportOptions(exerciseMatchMode = mode))

    @Test
    fun namesAreNormalizedToLowerCaseWords() {
        assertEquals("bench press barbell", normalizeExerciseName("  Bench-Press (Barbell) "))
    }

    @Test
    fun eachModeTriesHarderThanTheOneBefore() {
        val messy = "barbell bench-press"
        assertNull(matcher(ExerciseMatchMode.Exact).resolve(messy, ExerciseCategory.Strength))
        assertEquals(bench, matcher(ExerciseMatchMode.Normalized).resolve(messy, ExerciseCategory.Strength))

        assertNull(matcher(ExerciseMatchMode.Normalized).resolve("Bench Press", ExerciseCategory.Strength))
        assertEquals(bench, matcher(ExerciseMatchMode.Aliases).resolve("Bench Press", ExerciseCategory.Strength))

        assertNull(matcher(ExerciseMatchMode.Aliases).resolve("Cable Row, Seated", ExerciseCategory.Strength))
        assertEquals(row, matcher(ExerciseMatchMode.Fuzzy).resolve("Cable Row, Seated", ExerciseCategory.Strength))
    }

    @Test
    fun alwaysCreateNeverMatches() {
        assertNull(matcher(ExerciseMatchMode.AlwaysCreate).resolve(bench.name, ExerciseCategory.Strength))
    }

    @Test
    fun fuzzyMatchingStaysInTheSameCategory() {
        assertNull(matcher(ExerciseMatchMode.Fuzzy).resolve("Cable Row, Seated", ExerciseCategory.Cardio))
    }
}
