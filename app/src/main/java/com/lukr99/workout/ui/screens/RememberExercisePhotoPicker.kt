package com.lukr99.workout.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Camera and photo library for the exercise editor. [onPhoto] gets the chosen photo's bytes;
 * [onError] gets a message when the camera or the file could not be used.
 *
 * Each platform supplies this file; the web edition's is under web/ (`<input type=file capture>`).
 */
@Composable
fun rememberExercisePhotoPicker(
    onPhoto: (ByteArray) -> Unit,
    onError: (String) -> Unit,
): ExercisePhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCapture by remember { mutableStateOf<Pair<Uri, File>?>(null) }

    val library = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("The selected image could not be opened.")
                    }
                }.onSuccess(onPhoto).onFailure { onError(it.message ?: "Could not copy the selected photo") }
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = pendingCapture
        pendingCapture = null
        if (target == null) return@rememberLauncherForActivityResult
        val file = target.second
        if (!saved) {
            file.delete()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    file.readBytes().also { file.delete() }.also { require(it.isNotEmpty()) { "The camera did not return an image." } }
                }
            }.onSuccess(onPhoto).onFailure { onError(it.message ?: "Could not save the camera photo") }
        }
    }

    return remember(context) {
        ExercisePhotoPicker(
            takePhoto = {
                runCatching { captureTarget(context) }
                    .onSuccess {
                        pendingCapture = it
                        camera.launch(it.first)
                    }
                    .onFailure { onError(it.message ?: "Could not open the camera") }
            },
            choosePhoto = {
                library.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

private fun captureTarget(context: Context): Pair<Uri, File> {
    val directory = File(context.cacheDir, "exercise-photo-captures").also(File::mkdirs)
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    check(file.createNewFile()) { "Could not create a camera image target." }
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file) to file
}
