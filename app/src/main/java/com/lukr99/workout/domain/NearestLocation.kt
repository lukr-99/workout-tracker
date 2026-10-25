package com.lukr99.workout.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** How close a fix must be to a saved place to count as "near" it. */
const val NEAR_LOCATION_METERS = 150.0

/** Great-circle distance between two points in metres. */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val earthRadius = 6_371_000.0
    val lat1 = a.latitude.toRadians()
    val lat2 = b.latitude.toRadians()
    val dLat = lat2 - lat1
    val dLng = (b.longitude - a.longitude).toRadians()
    val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
    return 2 * earthRadius * asin(min(1.0, sqrt(h)))
}

/** The saved position of this place, or null when it has none. */
val Location.point: GeoPoint?
    get() {
        val lat = latitude ?: return null
        val lng = longitude ?: return null
        return GeoPoint(lat, lng)
    }

/**
 * The place nearest to [fix] within [maxMeters], or null. Outdoors never matches (it has no fixed
 * position), nor do archived places or places without a saved position. A tie on distance goes to
 * the name that sorts first, then the id, so the answer does not depend on list order.
 */
fun List<Location>.nearest(fix: GeoPoint, maxMeters: Double = NEAR_LOCATION_METERS): NearLocation? =
    asSequence()
        .filter { it.kind != LocationKind.Outdoors && !it.isArchived }
        .mapNotNull { place -> place.point?.let { NearLocation(place, distanceMeters(fix, it)) } }
        .filter { it.meters <= maxMeters }
        .minWithOrNull(compareBy<NearLocation> { it.meters }.thenBy { it.location.name.lowercase() }.thenBy { it.location.id })
