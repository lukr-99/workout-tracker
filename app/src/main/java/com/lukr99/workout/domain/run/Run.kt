package com.lukr99.workout.domain.run

import com.lukr99.workout.domain.newId
import kotlinx.serialization.Serializable

/** A completed run: summary metrics + a denormalized polyline, optionally with its full [trace]. */
@Serializable
data class Run(
    val id: String = newId(),
    /** Optional link to a Cardio [com.lukr99.workout.domain.WorkoutSession] for unified history. */
    val sessionId: String? = null,
    val startedAtUtc: Long = 0L,
    val durationSeconds: Long = 0L,
    val movingSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val calories: Double? = null,
    val avgHr: Int? = null,
    val source: RunSource = RunSource.Local,
    val externalKey: String? = null,
    /** Google-encoded polyline of the trace — the fast path for map thumbnails. */
    val encodedPolyline: String = "",
    val routeId: String? = null,
    val notes: String = "",
    /** Full raw trace; empty in list/summary reads, populated for detail + export. */
    val trace: List<TracePoint> = emptyList(),
)
