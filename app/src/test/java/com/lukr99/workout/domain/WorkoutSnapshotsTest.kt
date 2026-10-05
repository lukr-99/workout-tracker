package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSnapshotsTest {

    @Test
    fun aNewEntryCopiesTheExerciseAndStartsWithOneSet() {
        val bench = Exercise(id = "bench", name = "Bench Press", primaryBodyPart = "Chest")

        val entry = bench.toNewEntry(sortOrder = 2)

        assertEquals("bench", entry.exerciseId)
        assertEquals("Bench Press", entry.exerciseSnapshotName)
        assertEquals("Chest", entry.exerciseSnapshotPrimaryBodyPart)
        assertEquals(2, entry.sortOrder)
        assertEquals(listOf(1), entry.strengthSets.map { it.setNumber })
        assertNull(entry.cardioData)
    }

    @Test
    fun aCardioEntryStartsWithCardioDataAndNoSets() {
        val bike = Exercise(name = "Bike", category = ExerciseCategory.Cardio)

        val entry = bike.toNewEntry()

        assertTrue(entry.strengthSets.isEmpty())
        assertNotNull(entry.cardioData)
    }

    @Test
    fun templateEntriesFollowTemplateOrderAndKeepNotes() {
        val template = WorkoutTemplate(
            name = "Push",
            exercises = listOf(
                WorkoutTemplateExercise(exerciseId = "b", exerciseName = "Dips", sortOrder = 5),
                WorkoutTemplateExercise(exerciseId = "a", exerciseName = "Bench", sortOrder = 1, notes = "Pause"),
            ),
        )

        val entries = template.toEntries("session")

        assertEquals(listOf("Bench", "Dips"), entries.map { it.exerciseSnapshotName })
        assertEquals(listOf(0, 1), entries.map { it.sortOrder })
        assertEquals("Pause", entries.first().notes)
        assertTrue(entries.all { it.workoutSessionId == "session" })
    }

    @Test
    fun aTemplateFromAWorkoutKeepsCatalogExercisesOnly() {
        val workout = WorkoutSession(
            name = "Monday",
            startedAtUtc = 0,
            entries = listOf(
                WorkoutEntry(exerciseId = "a", exerciseSnapshotName = "Bench", sortOrder = 0),
                WorkoutEntry(exerciseId = "", exerciseSnapshotName = "Free text", sortOrder = 1),
            ),
        )

        assertEquals("Monday Copy", workout.toTemplate().name)
        val named = workout.toTemplate(" Push day ")
        assertEquals("Push day", named.name)
        assertEquals(listOf("Bench"), named.exercises.map { it.exerciseName })
    }
}
