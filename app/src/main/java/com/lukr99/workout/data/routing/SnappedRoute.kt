package com.lukr99.workout.data.routing

import com.lukr99.workout.domain.run.RoutePoint

/** A road/path-snapped route: ordered points + the provider's distance (metres). */
data class SnappedRoute(
    val points: List<RoutePoint>,
    val distanceMeters: Double,
)
