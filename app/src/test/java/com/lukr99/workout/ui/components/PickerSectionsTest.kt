package com.lukr99.workout.ui.components

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerSectionsTest {

    private val bench = Exercise(id = "bench", name = "Bench press", primaryBodyPart = "Chest")
    private val row = Exercise(id = "row", name = "Barbell row", primaryBodyPart = "Back")
    private val fly = Exercise(id = "fly", name = "Cable fly", primaryBodyPart = "Chest")
    private val catalog = listOf(bench, row, fly)

    @Test
    fun recentComeFirstInRecentOrderAndAreNotRepeated() {
        val sections = PickerSections.of(catalog, ExerciseFilter(), recentIds = listOf("fly", "bench", "gone"))

        assertEquals(listOf("fly", "bench"), sections.recent.map { it.id })
        assertEquals(listOf("row"), sections.all.map { it.id })
        assertNull(sections.createName)
    }

    @Test
    fun aSearchListsEveryMatchInOneBlock() {
        val sections = PickerSections.of(catalog, ExerciseFilter(searchText = "chest"), recentIds = listOf("fly"))

        assertTrue(sections.recent.isEmpty())
        assertEquals(setOf("bench", "fly"), sections.all.map { it.id }.toSet())
    }

    @Test
    fun aNewNameIsOfferedForCreateButAnExactNameIsNot() {
        assertEquals("Landmine press", PickerSections.of(catalog, ExerciseFilter(searchText = " Landmine press "), emptyList()).createName)
        assertNull(PickerSections.of(catalog, ExerciseFilter(searchText = "bench PRESS"), emptyList()).createName)
    }

    @Test
    fun filtersStillApplyToRecent() {
        val sections = PickerSections.of(catalog, ExerciseFilter(bodyPart = "Back"), recentIds = listOf("fly", "row"))

        assertEquals(listOf("row"), sections.recent.map { it.id })
        assertTrue(sections.all.isEmpty())
    }
}
