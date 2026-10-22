package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/**
 * A note for one exercise at one place, like "double pulley, seat 4". Machines differ between
 * gyms even when the numbers are shared, so the note belongs to the (exercise, location) pair and
 * works for shared and per-location exercises alike.
 */
@Serializable
data class ExerciseLocationNote(
    val exerciseId: String,
    val locationId: String,
    val note: String,
)
