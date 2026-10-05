package com.lukr99.workout.data

/** Room projection for [WorkoutDao.getLatestEntryNote]: a note and when its workout finished. */
data class PreviousNoteRow(
    val note: String,
    val atUtc: Long,
)
