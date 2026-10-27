package com.lukr99.workout.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.lukr99.workout.web.platform.PHOTO_MAX_EDGE
import com.lukr99.workout.web.platform.PHOTO_QUALITY
import com.lukr99.workout.web.platform.pickPhoto
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/**
 * Camera and photo library for the exercise editor, over `file input that accepts images`.
 * "Take photo" asks for the camera (`capture`); "Choose" opens the photo library. Either way the
 * image is made upright, scaled to at most [PHOTO_MAX_EDGE] pixels on its long side and written as
 * a JPEG, so [onPhoto] gets a few hundred kB, not a 12 MP original.
 *
 * Each platform supplies this file.
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun rememberExercisePhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit,
): ExercisePhotoPicker {
    val scope = rememberCoroutineScope()
    val photo by rememberUpdatedState(onPhoto)
    val failed by rememberUpdatedState(onError)

    return remember(scope) {
        fun open(capture: Boolean) {
            // The chooser has to open inside the tap, so the promise starts here and only the wait is launched.
            val chosen = runCatching { pickPhoto(capture, PHOTO_MAX_EDGE, PHOTO_QUALITY) }
            scope.launch {
                val outcome = chosen.mapCatching { it.await<JsString>().toString() }.getOrNull()
                when {
                    outcome == null -> failed("Could not open the photo chooser.")
                    outcome == "cancel" -> Unit
                    outcome.startsWith("ok:") -> {
                        val bytes = runCatching { Base64.Default.decode(outcome.removePrefix("ok:")) }.getOrNull()
                        if (bytes == null || bytes.isEmpty()) failed("The photo could not be read.") else photo(bytes)
                    }
                    else -> failed("The photo could not be read: ${outcome.removePrefix("error:")}")
                }
            }
        }
        ExercisePhotoPicker(takePhoto = { open(capture = true) }, choosePhoto = { open(capture = false) })
    }
}
