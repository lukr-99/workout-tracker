package com.lukr99.workout.domain.run

import kotlinx.serialization.Serializable

/** One point of a planned/saved route. [seq] orders the polyline. */
@Serializable
data class RoutePoint(
    val seq: Int,
    val lat: Double,
    val lon: Double,
    val elevationM: Double? = null,
)
