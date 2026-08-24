package com.lukr99.workout.data.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreeExerciseImageIndexTest {
    private fun entry(tag: String) = FreeExerciseImageEntry(images = listOf("$tag/0.jpg"), muscle = tag)

    @Test
    fun exactNormalizedKeyWins() {
        val index = FreeExerciseImageIndex.forTesting(mapOf("cable fly" to entry("exact")))
        assertEquals("exact", index.find("Cable Fly")?.muscle)
    }

    @Test
    fun singularizedTokensMatchPluralKey() {
        // "Hammer Curl" -> tokens {hammer, curl}; only the plural key survives after singularizing.
        val index = FreeExerciseImageIndex.forTesting(mapOf("hammer curls" to entry("plural")))
        assertEquals("plural", index.find("Hammer Curl")?.muscle)
    }

    @Test
    fun uniqueSupersetKeyMatchesEvenWithExtraTokens() {
        val index = FreeExerciseImageIndex.forTesting(
            mapOf("goblet squat hold" to entry("goblet"), "back squat" to entry("back")),
        )
        assertEquals("goblet", index.find("Goblet Squat")?.muscle)
    }

    @Test
    fun ambiguousMatchReturnsNull() {
        val index = FreeExerciseImageIndex.forTesting(
            mapOf("incline chest press" to entry("incline"), "decline chest press" to entry("decline")),
        )
        assertNull(index.find("Chest Press"))
    }

    @Test
    fun singleTokenQueryDoesNotGuess() {
        val index = FreeExerciseImageIndex.forTesting(mapOf("romanian deadlift" to entry("rdl")))
        assertNull(index.find("Deadlift"))
    }
}
