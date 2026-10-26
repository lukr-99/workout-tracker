package com.lukr99.workout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

/**
 * Draws one exercise picture from a [model] that [userPhotoModel] or the image resolver produced
 * (a URL string or a stored photo). Each platform supplies this file; the web edition's is under
 * web/.
 */
@Composable
fun ExerciseImage(model: Any, contentDescription: String?, modifier: Modifier) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** The image model for the user's own photo at [path], or null when the photo is gone. */
fun userPhotoModel(path: String): Any? = File(path).takeIf(File::isFile)
