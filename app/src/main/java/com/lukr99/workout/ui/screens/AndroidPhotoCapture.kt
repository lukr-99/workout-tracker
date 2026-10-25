package com.lukr99.workout.ui.screens

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.images.ExercisePhotoStore
import com.lukr99.workout.data.images.PhotoCaptureTarget
import com.lukr99.workout.ui.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Android-only camera and gallery photo flow for the exercise editor, kept out of the shared
 * LibraryViewModel because it needs Uri and FileProvider. The web edition does not compile this
 * file; it saves picked bytes through LibraryViewModel.saveExercisePhoto instead.
 */

private val LibraryViewModel.store: ExercisePhotoStore
    get() = photos as ExercisePhotoStore

fun LibraryViewModel.createPhotoCaptureTarget(): Result<PhotoCaptureTarget> =
    runCatching(store::createCaptureTarget)

fun LibraryViewModel.commitCapturedPhoto(
    exerciseId: String,
    target: PhotoCaptureTarget,
    onResult: (Result<String>) -> Unit,
) {
    viewModelScope.launch {
        onResult(runCatching {
            withContext(Dispatchers.IO) { store.commitCapture(exerciseId, target.temporaryPath) }
        })
    }
}

fun LibraryViewModel.importExercisePhoto(
    exerciseId: String,
    uri: Uri,
    onResult: (Result<String>) -> Unit,
) {
    viewModelScope.launch {
        onResult(runCatching {
            withContext(Dispatchers.IO) { store.importPhoto(exerciseId, uri) }
        })
    }
}

fun LibraryViewModel.discardPhotoCapture(target: PhotoCaptureTarget?) =
    store.discardCapture(target?.temporaryPath)
