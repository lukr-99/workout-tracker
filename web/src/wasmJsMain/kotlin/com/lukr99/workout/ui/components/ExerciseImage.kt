package com.lukr99.workout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Draws one exercise picture from a [model]. Each platform supplies this file. The web edition draws
 * no pictures yet, so this draws nothing and the caller's body-part placeholder shows through.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ExerciseImage(model: Any, contentDescription: String?, modifier: Modifier) = Unit

/** The image model for the user's own photo at [path]. The web edition has no photos yet, so null. */
@Suppress("UNUSED_PARAMETER")
fun userPhotoModel(path: String): Any? = null
