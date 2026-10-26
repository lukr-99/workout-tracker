package com.lukr99.workout.ui.screens

import com.lukr99.workout.ui.LibraryViewModel
import java.io.File

/**
 * Stopgap until `LibraryViewModel.saveExercisePhoto` (the ui branch) is merged: the member wins
 * when both exist, so this file can be deleted then. It saves bytes through the camera-capture path.
 */
fun LibraryViewModel.saveExercisePhoto(exerciseId: String, bytes: ByteArray, onResult: (Result<String>) -> Unit) {
    createPhotoCaptureTarget()
        .onFailure { onResult(Result.failure(it)) }
        .onSuccess { target ->
            runCatching { File(target.temporaryPath).writeBytes(bytes) }
                .onFailure {
                    discardPhotoCapture(target)
                    onResult(Result.failure(it))
                }
                .onSuccess { commitCapturedPhoto(exerciseId, target, onResult) }
        }
}
