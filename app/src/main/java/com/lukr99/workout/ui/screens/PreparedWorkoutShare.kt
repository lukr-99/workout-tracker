package com.lukr99.workout.ui.screens

import androidx.compose.ui.graphics.ImageBitmap

/** The exact share image the preview shows, and the call that sends that same image. */
class PreparedWorkoutShare(
    val image: ImageBitmap,
    /** Opens the share sheet with the image. Returns false when it could not be opened. */
    val share: () -> Boolean,
)
