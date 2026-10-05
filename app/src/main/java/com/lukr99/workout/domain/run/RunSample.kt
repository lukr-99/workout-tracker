package com.lukr99.workout.domain.run

/** One raw GPS fix handed to [RunTracker] (absolute epoch-millis [timeMs]). */
data class RunSample(
    val timeMs: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
    val speedMps: Double? = null,
    val elevationM: Double? = null,
)
