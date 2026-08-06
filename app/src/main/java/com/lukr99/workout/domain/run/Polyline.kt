package com.lukr99.workout.domain.run

import kotlin.math.roundToInt

/**
 * Google "encoded polyline algorithm" codec — pure Kotlin, unit-tested, no Android.
 *
 * Runs store their trace both as raw `run_points` (source of truth) and as a compact encoded polyline
 * on the `runs` row for fast map thumbnails ([Run.encodedPolyline]). This is that codec. Precision is
 * the standard 1e5 (~1 m), lossy in the last digit — round-tripping recovers coordinates to within
 * ~1e-5 degrees, which is exact for our storage purposes.
 *
 * A coordinate is `(lat, lon)`. See https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
object Polyline {

    private const val FACTOR = 1e5

    /** Encode an ordered list of `(lat, lon)` pairs into a polyline string. */
    fun encode(path: List<Pair<Double, Double>>): String {
        val sb = StringBuilder()
        var lastLat = 0
        var lastLon = 0
        for ((lat, lon) in path) {
            val iLat = (lat * FACTOR).roundToInt()
            val iLon = (lon * FACTOR).roundToInt()
            encodeValue(iLat - lastLat, sb)
            encodeValue(iLon - lastLon, sb)
            lastLat = iLat
            lastLon = iLon
        }
        return sb.toString()
    }

    /** Decode a polyline string back into `(lat, lon)` pairs. */
    fun decode(encoded: String): List<Pair<Double, Double>> {
        val out = ArrayList<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lon = 0
        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lon += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            out += (lat / FACTOR) to (lon / FACTOR)
        }
        return out
    }

    private fun encodeValue(value: Int, sb: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            sb.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        sb.append((v + 63).toChar())
    }

    // ── Trace/route convenience ──────────────────────────────────────────────

    fun encodeTrace(points: List<TracePoint>): String = encode(points.map { it.lat to it.lon })

    fun encodeRoute(points: List<RoutePoint>): String =
        encode(points.sortedBy { it.seq }.map { it.lat to it.lon })
}
