package com.lukr99.workout.ui

/** One row of Home's Recent list: a finished workout or a run, newest first. */
data class HomeRecent(
    val id: String,
    val isRun: Boolean,
    val name: String,
    val atUtc: Long,
    /** Lifted kilograms for a workout, metres for a run. */
    val amount: Double,
)
