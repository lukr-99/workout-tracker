package com.lukr99.workout.domain.run

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure pace / distance / split math for Run Mode — the running analogue of the strength `Estimates`.
 *
 * No Android, no I/O: given a trace ([TracePoint]s) or raw distance/time it derives everything the
 * UI shows (distance, speed, pace, per-km/mi splits, elevation gain) and formats it. All of it is
 * unit-tested. Distances are metres, durations seconds, pace **seconds per kilometre**.
 */
object Pace {

    private const val EARTH_RADIUS_M = 6_371_000.0
    const val METERS_PER_KM = 1_000.0
    const val METERS_PER_MILE = 1_609.344

    /** Great-circle distance between two lat/lon points, in metres (haversine). */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Total path length of a trace, in metres. A leg *into* a [TracePoint.segmentStart] point is a
     * paused-and-walked gap, so it contributes nothing — the distance stays continuous across breaks.
     */
    fun traceDistanceMeters(points: List<TracePoint>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            if (points[i].segmentStart) continue
            total += haversineMeters(
                points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon,
            )
        }
        return total
    }

    /** Total path length of a `(lat, lon)` polyline, in metres. */
    fun pathDistanceMeters(points: List<Pair<Double, Double>>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            total += haversineMeters(
                points[i - 1].first, points[i - 1].second, points[i].first, points[i].second,
            )
        }
        return total
    }

    /** Metres per second over a distance/duration. `0.0` when duration is non-positive. */
    fun speedMps(distanceMeters: Double, durationSeconds: Double): Double =
        if (durationSeconds > 0) distanceMeters / durationSeconds else 0.0

    /** Seconds per kilometre for a distance/duration. `0.0` when distance is non-positive. */
    fun paceSecPerKm(distanceMeters: Double, durationSeconds: Double): Double =
        if (distanceMeters > 0) durationSeconds / (distanceMeters / METERS_PER_KM) else 0.0

    /** Convert a per-km pace to per-mile. */
    fun paceSecPerMile(secPerKm: Double): Double = secPerKm * (METERS_PER_MILE / METERS_PER_KM)

    /**
     * Positive elevation change summed over the trace, in metres. [thresholdM] ignores jitter smaller
     * than a few metres so GPS altitude noise doesn't inflate the gain.
     */
    fun elevationGainMeters(points: List<TracePoint>, thresholdM: Double = 1.0): Double {
        var gain = 0.0
        var last: Double? = null
        for (p in points) {
            val e = p.elevationM ?: continue
            val prev = last
            if (prev != null) {
                val delta = e - prev
                if (delta >= thresholdM) {
                    gain += delta
                    last = e
                } else if (delta <= -thresholdM) {
                    last = e
                }
                // within threshold: hold `last` so small oscillations don't accumulate
            } else {
                last = e
            }
        }
        return gain
    }

    /**
     * Split the trace into [splitMeters] segments (a km or a mile), each with the pace held over it.
     * The crossing time at each boundary is linearly interpolated along the segment it falls in, so
     * splits are accurate even when GPS samples straddle the mark. A trailing remainder shorter than
     * a full split is emitted with `isFull = false`.
     */
    fun splits(points: List<TracePoint>, splitMeters: Double = METERS_PER_KM): List<Split> {
        if (points.size < 2 || splitMeters <= 0) return emptyList()
        val out = ArrayList<Split>()
        var cumDist = 0.0
        var lastMarkDist = 0.0
        var lastMarkTime = points.first().t.toDouble()
        var mark = splitMeters
        var index = 1

        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            // A leg into a segment start is a paused-and-walked gap: no distance, no split time.
            val seg = if (b.segmentStart) 0.0 else haversineMeters(a.lat, a.lon, b.lat, b.lon)
            if (seg <= 0.0) continue
            val segStart = cumDist
            val segEnd = cumDist + seg
            while (mark <= segEnd) {
                val frac = (mark - segStart) / seg
                val markTime = a.t + frac * (b.t - a.t)
                val durMs = markTime - lastMarkTime
                val durSec = durMs / 1000.0
                val dist = mark - lastMarkDist
                out += Split(
                    index = index,
                    distanceMeters = dist,
                    durationSeconds = durSec.roundToLong(),
                    paceSecPerKm = paceSecPerKm(dist, durSec),
                    isFull = true,
                )
                lastMarkTime = markTime
                lastMarkDist = mark
                mark += splitMeters
                index++
            }
            cumDist = segEnd
        }

        if (cumDist - lastMarkDist > 0.5) {
            val durSec = (points.last().t - lastMarkTime) / 1000.0
            val dist = cumDist - lastMarkDist
            out += Split(
                index = index,
                distanceMeters = dist,
                durationSeconds = durSec.roundToLong(),
                paceSecPerKm = paceSecPerKm(dist, durSec),
                isFull = false,
            )
        }
        return out
    }

    /** `"m:ss"` per km/mi (pace is minutes-per-unit); `"--:--"` for a non-positive pace. */
    fun formatPace(secPerUnit: Double): String {
        if (secPerUnit <= 0 || !secPerUnit.isFinite()) return "--:--"
        val total = secPerUnit.roundToInt()
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    /** `"H:MM:SS"` when there are hours, otherwise `"M:SS"`. */
    fun formatDuration(seconds: Long): String {
        val s = abs(seconds)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) {
            "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
        } else {
            "$m:${sec.toString().padStart(2, '0')}"
        }
    }
}
