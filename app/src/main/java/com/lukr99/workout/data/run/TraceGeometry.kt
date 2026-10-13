package com.lukr99.workout.data.run

import com.lukr99.workout.domain.run.RunTrace
import com.lukr99.workout.domain.run.TracePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Pure geometry for drawing a run's trace on the share image. The trace is projected with a local
 * equirectangular fit (accurate at run scale) and letterboxed to keep its aspect, so the shape reads
 * true rather than stretched. Segments break at manual pauses, as [RunTrace.segments] does.
 */
object TraceGeometry {

    /** Projects [trace] into the box, one polyline of canvas points per segment. Empty below 2 points. */
    fun project(trace: List<TracePoint>, left: Float, top: Float, width: Float, height: Float): List<List<Pair<Float, Float>>> {
        if (trace.size < 2) return emptyList()
        val lat0 = trace.map { it.lat }.average()
        val mPerLon = 111_320.0 * cos(Math.toRadians(lat0))
        fun metres(p: TracePoint) = (p.lon - trace[0].lon) * mPerLon to (p.lat - trace[0].lat) * 110_540.0
        val xy = trace.map(::metres)
        val minX = xy.minOf { it.first }
        val minY = xy.minOf { it.second }
        val spanX = (xy.maxOf { it.first } - minX).coerceAtLeast(1.0)
        val spanY = (xy.maxOf { it.second } - minY).coerceAtLeast(1.0)
        val scale = minOf(width / spanX, height / spanY)
        val offX = left + (width - spanX * scale) / 2.0
        val offY = top + (height - spanY * scale) / 2.0
        val drawH = spanY * scale
        return RunTrace.segments(trace).map { segment ->
            segment.map { p ->
                val (x, y) = metres(p)
                // Flip y: north is up on the card.
                (offX + (x - minX) * scale).toFloat() to (offY + drawH - (y - minY) * scale).toFloat()
            }
        }
    }

    /** Total drawn length of [polylines], ignoring the gaps between segments. */
    fun length(polylines: List<List<Pair<Float, Float>>>): Float =
        polylines.sumOf { line -> line.zipWithNext { a, b -> dist(a, b).toDouble() }.sum() }.toFloat()

    /**
     * The point [fraction] (0 to 1) of the way along [polylines], with its direction of travel.
     * Null when the trace has no length.
     */
    fun poseAlong(polylines: List<List<Pair<Float, Float>>>, fraction: Float): TracePose? {
        val total = length(polylines)
        if (total <= 0f) return null
        var remaining = total * fraction.coerceIn(0f, 1f)
        val legs = polylines.flatMap { it.zipWithNext() }.filter { (a, b) -> dist(a, b) > 0f }
        for ((i, leg) in legs.withIndex()) {
            val (a, b) = leg
            val d = dist(a, b)
            if (remaining <= d || i == legs.lastIndex) {
                val t = (remaining / d).coerceIn(0f, 1f)
                val angle = Math.toDegrees(atan2((b.second - a.second).toDouble(), (b.first - a.first).toDouble()))
                return TracePose(a.first + (b.first - a.first) * t, a.second + (b.second - a.second) * t, angle.toFloat())
            }
            remaining -= d
        }
        return null
    }

    /** True when start and finish are closer than [threshold] pixels, so one marker should stand for both. */
    fun startMeetsFinish(start: Pair<Float, Float>, finish: Pair<Float, Float>, threshold: Float): Boolean =
        dist(start, finish) < threshold

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float =
        hypot(b.first - a.first, b.second - a.second)
}
