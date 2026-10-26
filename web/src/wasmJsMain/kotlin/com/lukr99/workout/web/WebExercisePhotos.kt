package com.lukr99.workout.web

import com.lukr99.workout.data.images.ExercisePhotos

/** The web edition stores no exercise photos yet: saving one fails with a message the editor shows. */
internal object WebExercisePhotos : ExercisePhotos {
    override fun savePhoto(exerciseId: String, bytes: ByteArray): String =
        error("Photos are not available in the web edition yet.")

    override fun removePhoto(path: String?) = Unit
}
