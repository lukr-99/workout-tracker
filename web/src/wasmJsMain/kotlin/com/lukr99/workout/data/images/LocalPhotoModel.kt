package com.lukr99.workout.data.images

/**
 * What an image loader should load for a user photo saved at [path], or null when no such file
 * exists. Each platform supplies this file. The web edition has no photos yet, so it is always null
 * and every exercise shows its body-part placeholder.
 */
@Suppress("UNUSED_PARAMETER")
internal fun localPhotoModel(path: String): Any? = null
