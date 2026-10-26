package com.lukr99.workout.data.images

import kotlinx.serialization.json.Json

private const val RawImageBase =
    "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

/**
 * Loads the bundled metadata once. The image bytes stay remote and are cached by Coil after first
 * view, keeping the APK small while making revisited thumbnails offline-friendly.
 */
class FreeExerciseImageIndex private constructor(
    private val loader: () -> Map<String, FreeExerciseImageEntry>,
) {
    /**
     * Reads the bundled index through [readFreeExerciseImageIndexJson], which on Android is the
     * classpath resource, so the app and JVM tests load the very same file (CodePrint
     * android-agent-workflow).
     */
    constructor(
        json: Json = Json { ignoreUnknownKeys = true },
    ) : this({
        json.decodeFromString<Map<String, FreeExerciseImageEntry>>(readFreeExerciseImageIndexJson())
    })

    private val cached: Map<String, FreeExerciseImageEntry> by lazy { loader() }

    private val tokenized: List<Pair<Set<String>, FreeExerciseImageEntry>> by lazy {
        cached.map { (key, entry) -> tokensOf(key) to entry }
    }

    fun find(name: String): FreeExerciseImageEntry? {
        val key = ExerciseNameNormalizer.normalize(name)
        cached[key]?.let { return it }
        // Conservative fallback for imported/custom names that don't hit a key exactly (e.g. plurals
        // or an extra equipment word): match only when the query's singularized tokens are a subset of
        // a *single* best index entry. Ambiguous queries fall through to the body-part monogram.
        return bestSubsetMatch(tokensOf(key))
    }

    private fun bestSubsetMatch(queryTokens: Set<String>): FreeExerciseImageEntry? {
        if (queryTokens.size < 2) return null // single-token queries are too ambiguous to guess.
        var best: FreeExerciseImageEntry? = null
        var bestExtra = Int.MAX_VALUE
        var tied = false
        for ((tokens, entry) in tokenized) {
            if (!tokens.containsAll(queryTokens)) continue
            val extra = tokens.size - queryTokens.size
            when {
                extra < bestExtra -> { best = entry; bestExtra = extra; tied = false }
                extra == bestExtra -> tied = true
            }
        }
        return if (tied) null else best
    }

    private fun tokensOf(normalized: String): Set<String> =
        normalized.split(' ').filter(String::isNotBlank).map { it.removeSuffix("s") }.toSet()

    fun imageUrl(name: String): String? = find(name)?.images?.firstOrNull()
        ?.takeIf(String::isNotBlank)
        ?.let { RawImageBase + it }

    companion object {
        /** Build directly from a parsed map, bypassing assets — for unit tests of the matcher. */
        internal fun forTesting(entries: Map<String, FreeExerciseImageEntry>) =
            FreeExerciseImageIndex({ entries })
    }
}
