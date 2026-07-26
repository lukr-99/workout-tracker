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
class FreeExerciseImageIndex(
    private val assetManager: AssetManager,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @Volatile
    private var cached: Map<String, FreeExerciseImageEntry>? = null

    fun find(name: String): FreeExerciseImageEntry? =
        entries()[ExerciseNameNormalizer.normalize(name)]

    fun imageUrl(name: String): String? = find(name)?.images?.firstOrNull()
        ?.takeIf(String::isNotBlank)
        ?.let { RawImageBase + it }

    private fun entries(): Map<String, FreeExerciseImageEntry> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: assetManager.open(AssetName).bufferedReader().use { reader ->
                json.decodeFromString<Map<String, FreeExerciseImageEntry>>(reader.readText())
            }.also { cached = it }
        }
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
