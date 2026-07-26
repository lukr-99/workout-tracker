package com.lukr99.workout.data.images

import java.text.Normalizer

/**
 * Shared catalog-image matching key. It deliberately removes only presentation punctuation and a
 * small set of equipment suffixes so unlike exercises do not collapse onto the same image.
 */
object ExerciseNameNormalizer {
    private val punctuation = Regex("[^a-z0-9\\s]")
    private val whitespace = Regex("\\s+")
    private val suffixes = setOf(
        "exercise",
        "machine",
        "with barbell",
        "with dumbbell",
        "with dumbbells",
        "barbell",
        "dumbbell",
        "dumbbells",
    )
    private val aliases = mapOf(
        "back squat" to "barbell full squat",
        "barbell bench press" to "barbell bench press medium grip",
        "biceps curl" to "dumbbell bicep curl",
        "incline dumbbell press" to "incline dumbbell press",
        "lat pulldown" to "wide grip lat pulldown",
        "lateral raise" to "side lateral raise",
        "overhead press" to "standing military press",
        "outdoor walk" to "trail running walking",
        "romanian deadlift" to "romanian deadlift with dumbbells",
        "seated cable row" to "seated cable rows",
        "stationary bike" to "bicycling stationary",
        "rowing" to "rowing stationary",
        "jump rope" to "rope jumping",
        "triceps pushdown" to "triceps pushdown",
        "treadmill run" to "running treadmill",
    )

    fun normalize(value: String): String {
        val ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(punctuation, " ")
            .replace(whitespace, " ")
            .trim()
        val withoutSuffix = suffixes
            .sortedByDescending(String::length)
            .firstOrNull { ascii.endsWith(" $it") }
            ?.let { ascii.removeSuffix(" $it").trim() }
            .orEmpty()
            .ifBlank { ascii }
        return aliases[withoutSuffix] ?: withoutSuffix
    }
}
