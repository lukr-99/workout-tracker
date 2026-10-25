package com.lukr99.workout.data

import com.lukr99.workout.domain.GeoPoint
import com.lukr99.workout.domain.Location

/** A [PlaceSuggester] for platforms without a position (the web edition, previews): never suggests. */
object NoPlaceSuggester : PlaceSuggester {
    override suspend fun suggest(locations: List<Location>): LocationSuggestion? = null
    override suspend fun currentFix(): GeoPoint? = null
    override fun hasPermission(): Boolean = false
}
