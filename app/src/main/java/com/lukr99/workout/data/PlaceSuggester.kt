package com.lukr99.workout.data

import com.lukr99.workout.domain.GeoPoint
import com.lukr99.workout.domain.Location

/**
 * What the ViewModels need from a device position: a workout-start suggestion and a fix for saving
 * a place. Android implements it with GPS; the web edition has no position and uses [NoPlaceSuggester].
 */
interface PlaceSuggester {
    /** Workout-start check. Never prompts for permission; null when there is nothing to suggest. */
    suspend fun suggest(locations: List<Location>): LocationSuggestion?

    /** A good fix for saving a place, or null. */
    suspend fun currentFix(): GeoPoint?

    /** Whether the app already has location permission. */
    fun hasPermission(): Boolean
}
