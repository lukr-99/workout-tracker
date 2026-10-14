package com.lukr99.workout.domain

/** A finished workout or a run on one day of Home's week, so tapping the day can open it. */
data class DayActivity(
    val id: String,
    val isRun: Boolean,
    val name: String,
    val atUtc: Long,
)
