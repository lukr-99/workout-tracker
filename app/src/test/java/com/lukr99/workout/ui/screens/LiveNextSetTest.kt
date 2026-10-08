package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveNextSetTest {

    private val bench = WorkoutEntry(
        id = "bench",
        exerciseSnapshotName = "Bench press",
        strengthSets = listOf(StrengthSet(id = "b1"), StrengthSet(id = "b2"), StrengthSet(id = "b3")),
    )
    private val fly = WorkoutEntry(id = "fly", exerciseSnapshotName = "Cable fly", strengthSets = listOf(StrengthSet(id = "f1")))
    private val bike = WorkoutEntry(id = "bike", entryType = ExerciseCategory.Cardio, cardioData = CardioEntryData())

    @Test
    fun theNextSetIsTheFirstNotDoneInTheFirstOpenExercise() {
        assertEquals("b2", nextSet(listOf(bench, fly), setOf("b1"))?.second?.id)
        assertEquals("f1", nextSet(listOf(bench, fly), setOf("b1", "b2", "b3"))?.second?.id)
    }

    @Test
    fun finishedAndCardioExercisesAreSkipped() {
        val finished = bench.copy(completedAtUtc = 1L)

        assertEquals("f1", nextSet(listOf(bike, finished, fly), emptySet())?.second?.id)
        assertNull(nextSet(listOf(bike), emptySet()))
    }

    @Test
    fun theLabelNamesTheSetOnceAnExerciseIsUnderWay() {
        assertEquals("Bench press", nextSetLabel(listOf(bench), emptySet()))
        assertEquals("set 2 of 3", nextSetLabel(listOf(bench), setOf("b1")))
        assertNull(nextSetLabel(listOf(fly), setOf("f1")))
    }

    @Test
    fun onlyExercisesAfterTheCurrentOneAreCompact() {
        assertEquals(setOf("fly", "bike"), compactEntryIds(listOf(bench, fly, bike), emptySet()))
        // Once bench is done, fly holds the next set and opens.
        val done = setOf("b1", "b2", "b3")
        assertEquals(setOf("bike"), compactEntryIds(listOf(bench.copy(completedAtUtc = 1L), fly, bike), done))
    }

    @Test
    fun startedOrTickedExercisesStayOpen() {
        val started = fly.copy(startedAtUtc = 5L)
        assertEquals(setOf("bike"), compactEntryIds(listOf(bench, started, bike), emptySet()))
        // A set ticked out of order also keeps its exercise open.
        assertEquals(setOf("bike"), compactEntryIds(listOf(bench, fly, bike), setOf("f1")))
    }

    @Test
    fun aSupersetPartnerOfTheCurrentExerciseStaysOpen() {
        val pairedBench = bench.copy(supersetGroup = 1)
        val pairedFly = fly.copy(supersetGroup = 1)
        assertEquals(setOf("bike"), compactEntryIds(listOf(pairedBench, pairedFly, bike), emptySet()))
    }

    @Test
    fun withOnlyCardioTheFirstOpenExerciseStaysOpen() {
        val row = bike.copy(id = "row")
        assertEquals(setOf("row"), compactEntryIds(listOf(bike, row), emptySet()))
    }
}
