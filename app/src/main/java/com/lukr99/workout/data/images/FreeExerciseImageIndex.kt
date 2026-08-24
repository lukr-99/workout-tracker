package com.lukr99.workout.data.images

import android.content.res.AssetManager
import com.lukr99.workout.domain.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val AssetName = "free_exercise_image_index.json"
private const val RawImageBase =
    "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

@Serializable
data class FreeExerciseImageEntry(
    val images: List<String> = emptyList(),
    val muscle: String? = null,
    val equipment: String? = null,
)

/**
 * Loads the bundled metadata once. The image bytes stay remote and are cached by Coil after first
 * view, keeping the APK small while making revisited thumbnails offline-friendly.
 */
class FreeExerciseImageIndex private constructor(
    private val loader: () -> Map<String, FreeExerciseImageEntry>,
) {
    constructor(
        assetManager: AssetManager,
        json: Json = Json { ignoreUnknownKeys = true },
    ) : this({
        assetManager.open(AssetName).bufferedReader().use { reader ->
            json.decodeFromString<Map<String, FreeExerciseImageEntry>>(reader.readText())
        }
    })

    @Volatile
    private var cached: Map<String, FreeExerciseImageEntry>? = null

    @Volatile
    private var tokenized: List<Pair<Set<String>, FreeExerciseImageEntry>>? = null

    fun find(name: String): FreeExerciseImageEntry? {
        val key = ExerciseNameNormalizer.normalize(name)
        entries()[key]?.let { return it }
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
        for ((tokens, entry) in tokenIndex()) {
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

    private fun tokenIndex(): List<Pair<Set<String>, FreeExerciseImageEntry>> {
        tokenized?.let { return it }
        return synchronized(this) {
            tokenized ?: entries().map { (key, entry) -> tokensOf(key) to entry }.also { tokenized = it }
        }
    }

    fun imageUrl(name: String): String? = find(name)?.images?.firstOrNull()
        ?.takeIf(String::isNotBlank)
        ?.let { RawImageBase + it }

    private fun entries(): Map<String, FreeExerciseImageEntry> {
        cached?.let { return it }
        return synchronized(this) { cached ?: loader().also { cached = it } }
    }

    companion object {
        /** Build directly from a parsed map, bypassing assets — for unit tests of the matcher. */
        internal fun forTesting(entries: Map<String, FreeExerciseImageEntry>) =
            FreeExerciseImageIndex({ entries })
    }
}

enum class ExerciseImageSource { UserPhoto, Wger, FreeExerciseDb }

data class ResolvedExerciseImage(
    val model: Any,
    val source: ExerciseImageSource,
    val attribution: String? = null,
)

/** The single user-photo -> wger -> open-dataset resolution order used by every thumbnail. */
class ExerciseImageResolver(
    private val freeIndex: FreeExerciseImageIndex,
) {
    fun resolve(exercise: Exercise): ResolvedExerciseImage? {
        exercise.localImagePath?.takeIf(String::isNotBlank)?.let { path ->
            val file = java.io.File(path)
            if (file.isFile) {
                return ResolvedExerciseImage(file, ExerciseImageSource.UserPhoto)
            }
        }
        exercise.imageUrl?.takeIf(String::isNotBlank)?.let { url ->
            return ResolvedExerciseImage(url, ExerciseImageSource.Wger, exercise.imageAttribution)
        }
        freeIndex.imageUrl(exercise.name)?.let { url ->
            return ResolvedExerciseImage(url, ExerciseImageSource.FreeExerciseDb, "free-exercise-db")
        }
        return null
    }
}
