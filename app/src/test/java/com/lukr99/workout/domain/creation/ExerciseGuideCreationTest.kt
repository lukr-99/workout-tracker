package com.lukr99.workout.domain.creation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** v8 exercise fields: how-to steps, the guide link, and keeping secondary muscles on save. */
class ExerciseGuideCreationTest {
    private val factory = WorkoutFactory(ids = IdGenerator { "id" }, clock = TimeProvider { 0L })

    @Test
    fun instructions_dropBlankLinesAndTrimSteps() {
        val result = factory.exercise(
            ExerciseDraft(name = "Bench", instructions = "  Feet flat \n\n Bar to lower chest\n  "),
        )

        assertTrue(result.isValid)
        assertEquals("Feet flat\nBar to lower chest", result.value.instructions)
        assertEquals(listOf("Feet flat", "Bar to lower chest"), result.value.instructionSteps)
    }

    @Test
    fun videoUrl_getsHttpsWhenPastedWithoutScheme() {
        val result = factory.exercise(ExerciseDraft(name = "Bench", videoUrl = " youtu.be/abc "))

        assertTrue(result.isValid)
        assertEquals("https://youtu.be/abc", result.value.videoUrl)
    }

    @Test
    fun videoUrl_blankMeansNoLink() {
        val result = factory.exercise(ExerciseDraft(name = "Bench", videoUrl = "   "))

        assertTrue(result.isValid)
        assertNull(result.value.videoUrl)
    }

    @Test
    fun videoUrl_rejectsNonWebAddresses() {
        listOf("javascript:alert(1)", "file:///sdcard/a.mp4", "not a link", "ftp://host.example/a").forEach { raw ->
            val result = factory.exercise(ExerciseDraft(name = "Bench", videoUrl = raw))
            assertFalse("$raw should be rejected", result.isValid)
            assertEquals("exercise.videoUrl", result.issues.single().path)
        }
    }

    @Test
    fun secondaryBodyParts_areKeptAndPrimaryIsNotRepeated() {
        val result = factory.exercise(
            ExerciseDraft(
                name = "Bench",
                primaryBodyPart = "Chest",
                secondaryBodyParts = listOf("Triceps", "chest", "Shoulders", "triceps"),
            ),
        )

        assertEquals(listOf("Triceps", "Shoulders"), result.value.secondaryBodyParts)
    }
}
