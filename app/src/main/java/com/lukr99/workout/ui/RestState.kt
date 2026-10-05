package com.lukr99.workout.ui

/** The rest countdown between sets, in whole seconds. */
data class RestState(
    val running: Boolean = false,
    val remaining: Int = 0,
    val total: Int = 0,
)
