package com.lukr99.workout.ui

/**
 * The live workout's question "Track [exerciseName] separately per gym?": its typical top set here
 * ([hereKg]) is far from the one at [otherLocationName] ([otherKg]).
 */
data class SplitPrompt(
    val exerciseId: String,
    val exerciseName: String,
    val hereKg: Double,
    val otherKg: Double,
    val otherLocationName: String,
)
