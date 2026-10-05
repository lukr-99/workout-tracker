package com.lukr99.workout.data.health

/** One point of a run's GPS route written as a Health Connect `ExerciseRoute.Location`. */
internal data class HealthRoutePoint(
    val timeUtcMillis: Long,
    val lat: Double,
    val lon: Double,
    val altitudeM: Double? = null,
)
