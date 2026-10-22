package com.lukr99.workout.domain

/**
 * Why [LocationSplitHeuristic] thinks an exercise should be tracked per location: its typical top
 * set here and at [otherLocationId] are far apart.
 */
data class LocationSplitSuggestion(
    val exerciseId: String,
    val locationId: String,
    val hereTopKg: Double,
    val otherLocationId: String,
    val otherTopKg: Double,
) {
    /** The larger weight over the smaller one, always at least 1. */
    val ratio: Double get() = maxOf(hereTopKg, otherTopKg) / minOf(hereTopKg, otherTopKg)
}
