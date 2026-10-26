package com.lukr99.workout.data.document

/** Stored data the app could not read, set aside under [key] so it can be offered as a download. */
data class QuarantineEntry(
    val key: String,
    val text: String,
    val reason: String,
    val atUtc: Long,
)
