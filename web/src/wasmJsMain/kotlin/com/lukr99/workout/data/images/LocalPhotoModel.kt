package com.lukr99.workout.data.images

import com.lukr99.workout.web.WebPhotos

/**
 * What an image loader should load for a user photo saved under [path], or null when no such photo
 * is stored. Each platform supplies this file. On the web the model is the stored photo itself.
 */
internal fun localPhotoModel(path: String): Any? = WebPhotos.model(path)
