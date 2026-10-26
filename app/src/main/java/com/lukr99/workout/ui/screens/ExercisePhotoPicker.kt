package com.lukr99.workout.ui.screens

/** The two ways to add an exercise photo. Each delivers the photo's bytes (JPEG or PNG). */
class ExercisePhotoPicker(
    /** Opens the camera. */
    val takePhoto: () -> Unit,
    /** Opens the photo library or file picker. */
    val choosePhoto: () -> Unit,
)
