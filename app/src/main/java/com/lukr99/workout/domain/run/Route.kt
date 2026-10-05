package com.lukr99.workout.domain.run

import com.lukr99.workout.domain.newId
import kotlinx.serialization.Serializable

/** A saved, reusable route (planned in R3; the model + storage land in R0). */
@Serializable
data class Route(
    val id: String = newId(),
    val name: String = "",
    val distanceMeters: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val encodedPolyline: String = "",
    val createdAtUtc: Long = 0L,
    val notes: String = "",
    val points: List<RoutePoint> = emptyList(),
)
