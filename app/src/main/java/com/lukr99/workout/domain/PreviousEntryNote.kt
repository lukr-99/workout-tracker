package com.lukr99.workout.domain

/**
 * The most recent note written on an exercise in a finished workout, shown as "Last time" while
 * logging the same exercise again.
 */
data class PreviousEntryNote(
    val exerciseId: String,
    val text: String,
    val atUtc: Long,
)
