package com.lukr99.workout.domain.run

import kotlinx.serialization.Serializable

/**
 * One raw GPS sample of a run's trace. [t] is a millisecond offset from the run's start (not an
 * absolute timestamp) so a trace is self-contained and cheap to diff. The denormalized
 * [Run.encodedPolyline] is the fast path for thumbnails; this is the source of truth for re-analysis
 * and GPX export.
 */
@Serializable
data class TracePoint(
    val t: Long,
    val lat: Double,
    val lon: Double,
    val elevationM: Double? = null,
    val speedMps: Double? = null,
    val hrBpm: Int? = null,
    val accuracyM: Double? = null,
    /**
     * True when this point **begins a new trace segment** — a manual pause (e.g. walking around an
     * obstacle) separates it from the previous point. The leg *into* a segment start is never drawn as
     * a connecting line nor counted toward distance, so a paused walk neither joins the route on the
     * map nor inflates the kilometres. Defaults false (a normal, connected point), keeping the
     * `@Serializable` wire/export shape backward-compatible.
     */
    val segmentStart: Boolean = false,
)
