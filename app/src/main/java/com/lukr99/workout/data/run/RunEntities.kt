package com.lukr99.workout.data.run

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lukr99.workout.domain.run.RunSource

/**
 * Room persistence shapes for Run Mode (schema **v6**, additive + non-destructive) — the running
 * counterpart to strength's [com.lukr99.workout.data.Entities]. These are an implementation detail
 * behind [RunRepository]; the app's vocabulary is `domain/run`, which the repository maps to/from.
 *
 * A run keeps both its raw [RunPointEntity] trace (source of truth, for re-analysis / GPX) **and** a
 * denormalized `encodedPolyline` on [RunEntity] for fast map thumbnails. Child points cascade with
 * their parent. `runs.routeId` / `runs.sessionId` are loose references (indexed, **not** foreign
 * keys) — deleting a route or a session must never cascade away a recorded run.
 */

@Entity(
    tableName = "runs",
    indices = [Index("sessionId"), Index("routeId"), Index("startedAtUtc"), Index("externalKey")],
)
data class RunEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val startedAtUtc: Long,
    val durationSeconds: Long,
    val movingSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double,
    val elevationGainM: Double,
    val calories: Double?,
    val avgHr: Int?,
    val source: RunSource,
    val externalKey: String?,
    val encodedPolyline: String,
    val routeId: String?,
    val notes: String,
)

@Entity(
    tableName = "run_points",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runId")],
)
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val t: Long,
    val lat: Double,
    val lon: Double,
    val elevationM: Double?,
    val speedMps: Double?,
    val hrBpm: Int?,
    val accuracyM: Double?,
    /** True when this point starts a new segment after a manual pause (see `TracePoint.segmentStart`). */
    val segmentStart: Boolean = false,
)

@Entity(
    tableName = "routes",
    indices = [Index("name")],
)
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val distanceMeters: Double,
    val elevationGainM: Double,
    val encodedPolyline: String,
    val createdAtUtc: Long,
    val notes: String,
)

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId")],
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val seq: Int,
    val lat: Double,
    val lon: Double,
    val elevationM: Double?,
)
