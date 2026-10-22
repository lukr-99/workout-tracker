package com.lukr99.workout.domain.creation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saving an exercise from the editor keeps how its history is read. */
class ExerciseLocationCreationTest {
    private val factory = WorkoutFactory(ids = IdGenerator { "id" }, clock = TimeProvider { 0L })

    @Test
    fun perGymFlagsSurviveASave() {
        val saved = factory.exercise(ExerciseDraft(name = "Cable Fly", trackPerLocation = true, splitPromptMuted = true)).value
        assertTrue(saved.trackPerLocation)
        assertTrue(saved.splitPromptMuted)
    }

    @Test
    fun newExercisesStartShared() {
        val saved = factory.exercise(ExerciseDraft(name = "Cable Fly")).value
        assertFalse(saved.trackPerLocation)
        assertFalse(saved.splitPromptMuted)
    }
}
