package com.lukr99.workout.domain.run

import com.lukr99.workout.domain.newId
import kotlinx.serialization.Serializable

/**
 * Portable Run Mode domain model — the running counterpart to the strength `domain/Models.kt`.
 *
 * Pure Kotlin with **no Android imports** and the same wire conventions as the strength model:
 * timestamps are epoch-millis UTC `Long`, enums serialize as their Int **ordinal**, and every field
 * has a default so the `@Serializable` shapes double as the export contract (`ExportBundle` `1.5`).
 * Room persistence lives in `data/run/RunEntities.kt`; [com.lukr99.workout.data.run.RunRepository]
 * maps between the two. Analytics (pace, splits, PRs) operate on these models and stay portable.
 */

/** Where a run originated. Ordinal-stable — appended-to only, never reordered (wire format). */
enum class RunSource { Local, Imported, HealthConnect }

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
)

/** A single km/mi split with the pace held over it. Derived by [Pace.splits] — never persisted raw. */
@Serializable
data class Split(
    val index: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val paceSecPerKm: Double,
    val elevationGainM: Double? = null,
    /** False for the final, shorter-than-a-full-split remainder. */
    val isFull: Boolean = true,
)

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

/** One point of a planned/saved route. [seq] orders the polyline. */
@Serializable
data class RoutePoint(
    val seq: Int,
    val lat: Double,
    val lon: Double,
    val elevationM: Double? = null,
)

/** A saved, reusable route (planned in R3; the model + storage land in R0). */
@Serializable
data class Route(
    val id: String = newId(),
    val name: String = "",
    val distanceMeters: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val encodedPolyline: String = "",
    val createdAtUtc: Long = 0L,
    val notes: String = "",
    val points: List<RoutePoint> = emptyList(),
)
