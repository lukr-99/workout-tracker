package com.lukr99.workout.domain.run

/**
 * Trace-shaping helpers for Run Mode. A run's trace is one flat, ordered list of [TracePoint]s that
 * may contain **segment breaks**: a point with [TracePoint.segmentStart] `== true` begins a new
 * segment because a manual pause (e.g. walking around an obstacle) separated it from the previous
 * point. Anything that *draws* the trace must break the line at those boundaries — otherwise a paused
 * walk shows as a straight line joining where you stopped to where you resumed. Distance math skips
 * those legs inline (see [Pace.traceDistanceMeters]); drawing uses [segments] to render one line per
 * contiguous stretch.
 */
object RunTrace {

    /**
     * Split [points] into contiguous runs of points, cutting before every [TracePoint.segmentStart]
     * (never before the first point). Each returned segment is a continuous line safe to draw as-is;
     * an empty trace yields an empty list.
     */
    fun segments(points: List<TracePoint>): List<List<TracePoint>> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<MutableList<TracePoint>>()
        var current = ArrayList<TracePoint>()
        points.forEachIndexed { i, p ->
            if (i > 0 && p.segmentStart && current.isNotEmpty()) {
                out += current
                current = ArrayList()
            }
            current += p
        }
        if (current.isNotEmpty()) out += current
        return out
    }
}
