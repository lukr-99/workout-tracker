package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class CardioEntryData(
    val workoutEntryId: String = "",
    val durationSeconds: Int = 0,
    val distanceKm: Double? = null,
    val calories: Double? = null,
    val notes: String = "",
)

// --- Projections (computed, not persisted) — ported from MAUI Models.cs -------------------------
