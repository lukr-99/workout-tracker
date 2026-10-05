package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {

    private val catalog = listOf(
        Exercise(name = "Row", primaryBodyPart = "Back", equipment = "Cable, Bar"),
        Exercise(name = "Bike", category = ExerciseCategory.Cardio, primaryBodyPart = "Cardio"),
        Exercise(name = "Curl", primaryBodyPart = "Arms", secondaryBodyParts = listOf("Back")),
        Exercise(name = "Old", primaryBodyPart = "Back", isArchived = true),
    )

    @Test
    fun theDefaultHidesArchivedAndSortsStrengthFirst() {
        assertEquals(listOf("Curl", "Row", "Bike"), ExerciseFilter().apply(catalog).map { it.name })
    }

    @Test
    fun aBodyPartMatchesSecondaryPartsToo() {
        assertEquals(listOf("Curl", "Row"), ExerciseFilter(bodyPart = "back").apply(catalog).map { it.name })
    }

    @Test
    fun equipmentMatchesOneItemOfAList() {
        assertEquals(listOf("Row"), ExerciseFilter(equipment = "bar").apply(catalog).map { it.name })
    }

    @Test
    fun archivedRowsShowWhenAskedFor() {
        val names = ExerciseFilter(searchText = "old", includeArchived = true).apply(catalog).map { it.name }
        assertEquals(listOf("Old"), names)
    }
}
