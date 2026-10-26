package com.lukr99.workout.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.lukr99.workout.web.WebPhotoModel
import com.lukr99.workout.web.WebPhotos
import com.lukr99.workout.web.WebPhotoImages

/**
 * Draws one exercise picture from a [model]. Each platform supplies this file. The web edition
 * draws the user's own photos ([WebPhotoModel]); catalog pictures live on another site, which the
 * page's content security policy does not allow, so any other model draws nothing and the caller's
 * body-part placeholder shows through.
 */
@Composable
fun ExerciseImage(model: Any, contentDescription: String?, modifier: Modifier) {
    val photo = model as? WebPhotoModel ?: return
    val bitmap = remember(photo.key) { WebPhotoImages.bitmap(photo) } ?: return
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/** The image model for the user's own photo with this key, or null when the photo is not stored. */
fun userPhotoModel(path: String): Any? = WebPhotos.model(path)
