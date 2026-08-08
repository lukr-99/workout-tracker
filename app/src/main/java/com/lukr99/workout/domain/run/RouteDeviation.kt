package com.lukr99.workout.domain.run

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Pure "how far off the planned route am I" math for a run started from a saved route. A planned
 * route is only ever a **faint reference** — it never constrains the run — so this is used purely for
 * an optional, non-intrusive indicator. Returns the shortest distance (metres) from a point to the
 * route polyline via local equirectangular projection (accurate for the tens-of-metres range that
 * matters here).
 */
object RouteDeviation {

    private const val METERS_PER_DEG_LAT = 110_540.0
    private const val METERS_PER_DEG_LON_EQ = 111_320.0

    /** Shortest distance (m) from `(lat, lon)` to the `(lat, lon)` [route] polyline; +∞ if empty. */
    fun distanceToRouteMeters(lat: Double, lon: Double, route: List<Pair<Double, Double>>): Double {
        if (route.isEmpty()) return Double.POSITIVE_INFINITY
        val mPerLon = METERS_PER_DEG_LON_EQ * cos(Math.toRadians(lat))
        // Point at the local origin; project route vertices into metres relative to it.
        fun x(p: Pair<Double, Double>) = (p.second - lon) * mPerLon
        fun y(p: Pair<Double, Double>) = (p.first - lat) * METERS_PER_DEG_LAT

        if (route.size == 1) {
            val dx = x(route[0]); val dy = y(route[0])
            return sqrt(dx * dx + dy * dy)
        }
        var best = Double.POSITIVE_INFINITY
        for (i in 1 until route.size) {
            best = minOf(best, pointToSegment(x(route[i - 1]), y(route[i - 1]), x(route[i]), y(route[i])))
        }
        return best
    }

    /** Distance from the origin (0,0) to segment (ax,ay)-(bx,by), in the same units as the inputs. */
    private fun pointToSegment(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        val t = if (lenSq <= 0.0) 0.0 else (-(ax * dx + ay * dy) / lenSq).coerceIn(0.0, 1.0)
        val cx = ax + t * dx
        val cy = ay + t * dy
        return sqrt(cx * cx + cy * cy)
    }
}
