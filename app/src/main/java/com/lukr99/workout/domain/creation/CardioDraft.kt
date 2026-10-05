package com.lukr99.workout.domain.creation

data class CardioDraft(
    val durationSeconds: Int = 0,
    val distanceKm: Double? = null,
    val calories: Double? = null,
    val notes: String = "",
)
