package com.lukr99.workout.domain.run

import kotlinx.serialization.Serializable

/** A single km/mi split with the pace held over it. Derived by [Pace.splits] — never persisted raw. */
@Serializable
data class Split(
    val index: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val paceSecPerKm: Double,
    val elevationGainM: Double? = null,
    /** False for the final, shorter-than-a-full-split remainder. */
    val isFull: Boolean = true,
)
