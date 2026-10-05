package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateUpdatesTest {

    private val push = WorkoutTemplate(
        id = "push",
        name = "Push day",
        exercises = listOf(
            WorkoutTemplateExercise(id = "t1", exerciseId = "bench", exerciseName = "Bench press", sortOrder = 0, targetSets = 3, repsMin = 6, repsMax = 8, restSeconds = 150, notes = "Pause"),
            WorkoutTemplateExercise(id = "t2", exerciseId = "dips", exerciseName = "Dips", sortOrder = 1),
        ),
    )

    private fun entry(id: String, name: String, sets: Int, order: Int, group: Int? = null) = WorkoutEntry(
        exerciseId = id,
        exerciseSnapshotName = name,
        sortOrder = order,
        supersetGroup = group,
        strengthSets = List(sets) { StrengthSet(setNumber = it + 1, reps = 8, weightKg = 80.0) },
    )

    private val workout = WorkoutSession(
        templateId = "push",
        entries = listOf(entry("bench", "Bench press", 4, 0), entry("fly", "Cable fly", 3, 1, group = 1)),
    )

    @Test
    fun aPlannedTemplateStartsWithItsSetsAndSupersets() {
        val planned = push.copy(exercises = push.exercises.map { it.copy(supersetGroup = 2) })

        val entries = planned.toEntries("w")

        assertEquals(3, entries[0].strengthSets.size)
        assertEquals(listOf(8, 8, 8), entries[0].strengthSets.map { it.reps })
        assertEquals(1, entries[1].strengthSets.size)
        assertEquals(listOf(2, 2), entries.map { it.supersetGroup })
    }

    @Test
    fun changesListAddedSkippedAndOtherSetCounts() {
        val changes = push.changesIn(workout)

        assertEquals(
            listOf(
                TemplateChange.Added("Cable fly"),
                TemplateChange.Skipped("Dips"),
                TemplateChange.SetCount("Bench press", done = 4, planned = 3),
            ),
            changes,
        )
    }

    @Test
    fun aWorkoutDoneAsPlannedHasNoChanges() {
        val asPlanned = workout.copy(entries = listOf(entry("bench", "Bench press", 3, 0), entry("dips", "Dips", 1, 1)))

        assertTrue(push.changesIn(asPlanned).isEmpty())
    }

    @Test
    fun theUpdatedTemplateFollowsTheWorkoutAndKeepsThePlanDetails() {
        val updated = push.updatedFrom(workout)

        assertEquals("push", updated.id)
        assertEquals(listOf("bench", "fly"), updated.exercises.map { it.exerciseId })
        val bench = updated.exercises[0]
        assertEquals(4, bench.targetSets)
        assertEquals(6, bench.repsMin)
        assertEquals(150, bench.restSeconds)
        assertEquals("Pause", bench.notes)
        assertEquals("t1", bench.id)
        val fly = updated.exercises[1]
        assertEquals(3, fly.targetSets)
        assertEquals(1, fly.supersetGroup)
        assertEquals(1, fly.sortOrder)
    }
}
