package com.lukr99.workout.ui

import com.lukr99.workout.domain.GeoPoint
import com.lukr99.workout.domain.Location

/** Offer to save [fix] as the position of [location]. */
data class PositionOffer(val location: Location, val fix: GeoPoint)
