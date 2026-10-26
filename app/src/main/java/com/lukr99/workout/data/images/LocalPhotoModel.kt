package com.lukr99.workout.data.images

import java.io.File

/**
 * What an image loader should load for a user photo saved at [path], or null when no such file
 * exists. Each platform supplies this file; the web edition's is under web/. On Android the model
 * is the [File].
 */
internal fun localPhotoModel(path: String): Any? = File(path).takeIf { it.isFile }
