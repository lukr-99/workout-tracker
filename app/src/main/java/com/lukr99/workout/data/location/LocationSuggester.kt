package com.lukr99.workout.data.location

import com.lukr99.workout.domain.GeoPoint
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.NEAR_LOCATION_METERS
import com.lukr99.workout.domain.nearest

/**
 * Reads one position and matches it to the saved places. It never blocks anything: with no
 * permission, no fix in time, or a provider that throws, it returns null and the caller keeps the
 * last used location.
 */
class LocationSuggester(
    private val provider: PositionProvider,
    private val maxMeters: Double = NEAR_LOCATION_METERS,
    private val startTimeoutMs: Long = START_TIMEOUT_MS,
) {
    /** Workout-start check. Does not prompt for permission. */
    suspend fun suggest(locations: List<Location>): LocationSuggestion? {
        if (!provider.hasPermission()) return null
        val fix = runCatching { provider.currentPosition(FixQuality.Balanced, startTimeoutMs) }.getOrNull() ?: return null
        return LocationSuggestion(fix, locations.nearest(fix, maxMeters)?.location)
    }

    /** A good fix for saving a place, or null. */
    suspend fun currentFix(): GeoPoint? {
        if (!provider.hasPermission()) return null
        return runCatching { provider.currentPosition(FixQuality.High, SAVE_TIMEOUT_MS) }.getOrNull()
    }

    fun hasPermission(): Boolean = provider.hasPermission()

    companion object {
        const val START_TIMEOUT_MS = 4_000L
        const val SAVE_TIMEOUT_MS = 15_000L
    }
}
