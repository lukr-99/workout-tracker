package com.lukr99.workout.data.transfer

import com.lukr99.workout.data.export.ExercisePhoto
import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.run.Run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplacePlannerTest {

    private val current = StoreCounts(exercises = 20, templates = 2, workouts = 40, runs = 5, routes = 1)

    @Test
    fun replace_keepsEveryRecordExactlyIncludingSameNamedExercises() {
        val payload = ImportedPayload(
            format = DataFormat.WorkoutJson,
            exercises = listOf(Exercise(id = "a", name = "Row"), Exercise(id = "b", name = "Row")),
            sessions = listOf(WorkoutSession(id = "s1"), WorkoutSession(id = "s2")),
            runs = listOf(Run(id = "r1")),
            photos = listOf(ExercisePhoto(exerciseId = "a", dataBase64 = "AA==")),
            settings = SettingsSnapshot(units = "Imperial"),
        )

        val preview = ReplacePlanner.plan(payload, current)

        assertTrue(preview.canCommit)
        assertEquals(RestoreMode.Replace, preview.plan.mode)
        assertEquals(listOf("a", "b"), preview.plan.exercises.map { it.value.id })
        assertTrue(preview.plan.exercises.all { it.action == PlannedAction.Insert })
        assertEquals(listOf("s1", "s2"), preview.plan.sessions.map { it.value.id })
        assertEquals(1, preview.plan.runs.size)
        assertEquals(current, preview.plan.replaces)
        assertEquals("Imperial", preview.plan.settings?.units)
        assertEquals(mapOf("a" to "a", "b" to "b"), preview.plan.exerciseIdMap)
        assertEquals(1, preview.summary.photos)
    }

    @Test
    fun replace_refusesAnythingButAnEmberBackup() {
        val preview = ReplacePlanner.plan(ImportedPayload(format = DataFormat.LyftaCsv), current)

        assertFalse(preview.canCommit)
        assertEquals("replace.format", preview.plan.issues.single().code)
    }
}
