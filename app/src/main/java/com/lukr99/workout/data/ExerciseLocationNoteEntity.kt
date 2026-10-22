package com.lukr99.workout.data

import androidx.room.Entity

/** v11: a machine note for one exercise at one location. At most one row per pair. */
@Entity(tableName = "exercise_location_notes", primaryKeys = ["exerciseId", "locationId"])
data class ExerciseLocationNoteEntity(
    val exerciseId: String,
    val locationId: String,
    val note: String,
)
