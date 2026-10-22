package com.lukr99.workout.ui

/** One line on the "All gyms" chart: an exercise's workouts at a single place. */
data class LocationSeries(
    /** Null is "Unknown location": workouts saved without a place. */
    val locationId: String?,
    val name: String,
    val points: List<ProgressPoint>,
) {
    val bestE1rmKg: Double get() = points.maxOfOrNull { it.e1rmKg } ?: 0.0
    val latestE1rmKg: Double get() = points.lastOrNull()?.e1rmKg ?: 0.0
}
