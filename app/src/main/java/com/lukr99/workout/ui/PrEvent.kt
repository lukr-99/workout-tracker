package com.lukr99.workout.ui

/** A new personal record set live. The screen consumes it after the celebration plays. */
data class PrEvent(
    val id: Long,
    val exerciseName: String,
    val estimated1RmKg: Double,
    val headline: String,
)
