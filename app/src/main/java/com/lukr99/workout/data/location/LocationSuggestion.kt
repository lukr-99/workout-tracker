package com.lukr99.workout.data.location

import com.lukr99.workout.domain.GeoPoint
import com.lukr99.workout.domain.Location

/** What a fix at workout start says: the fix itself, and the saved place it is near, if any. */
data class LocationSuggestion(val fix: GeoPoint, val near: Location?)
