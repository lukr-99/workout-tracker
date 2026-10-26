package com.lukr99.workout.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

private const val NOT_AVAILABLE = "Photos are not available in the web edition yet."

/**
 * Camera and photo library for the exercise editor. The web edition has no photos yet, so both
 * actions tell the person so through [onError]. [onPhoto] is never called.
 *
 * Each platform supplies this file.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun rememberExercisePhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit,
): ExercisePhotoPicker {
    val failed = rememberUpdatedState(onError)
    return remember { ExercisePhotoPicker(takePhoto = { failed.value(NOT_AVAILABLE) }, choosePhoto = { failed.value(NOT_AVAILABLE) }) }
}
