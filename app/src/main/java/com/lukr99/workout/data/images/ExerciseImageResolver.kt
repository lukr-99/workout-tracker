package com.lukr99.workout.data.images

import com.lukr99.workout.domain.Exercise

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
