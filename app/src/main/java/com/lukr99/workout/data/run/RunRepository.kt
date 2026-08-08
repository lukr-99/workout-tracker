package com.lukr99.workout.data.run

import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single API over run/route persistence — the **only** class that touches Room for Run Mode
 * (mirrors [com.lukr99.workout.data.WorkoutRepository] for strength). It maps the Room `@Entity`
 * shapes to/from the portable `domain/run` model and owns the polyline encode/decode.
 *
 * R0 scope: read/write skeleton + list flows. Live-run persistence (incremental point writes,
 * moving-time/split derivation) arrives in R1; Health Connect export in R2.
 */
class RunRepository(private val dao: RunDao) {

    // --- Runs ----------------------------------------------------------------------------------

    /** Recent-first run summaries (no trace loaded — the encoded polyline covers thumbnails). */
    fun observeRuns(): Flow<List<Run>> = dao.observeRuns().map { rows -> rows.map { it.toDomain() } }

    suspend fun getRuns(): List<Run> = dao.getAllRuns().map { it.toDomain() }

    suspend fun countRuns(): Int = dao.countRuns()

    /** All runs with their full traces loaded — for stats/PRs that need sub-run windows. */
    suspend fun getRunsWithTraces(): List<Run> = dao.getAllRuns().map { row ->
        row.toDomain(dao.getRunPoints(row.id).map { it.toDomain() })
    }

    /** A run with its full raw trace loaded — for detail, editing, or GPX export. */
    suspend fun getRun(id: String): Run? {
        val row = dao.getRun(id) ?: return null
        val trace = dao.getRunPoints(id).map { it.toDomain() }
        return row.toDomain(trace)
    }

    /**
     * Persist a run and (re)write its raw trace. The `encodedPolyline` is derived from [run.trace]
     * when present so the stored fast-path always matches the points.
     */
    suspend fun saveRun(run: Run) {
        val encoded = if (run.trace.isNotEmpty()) Polyline.encodeTrace(run.trace) else run.encodedPolyline
        dao.upsertRun(run.copy(encodedPolyline = encoded).toEntity())
        if (run.trace.isNotEmpty()) {
            dao.upsertRunPoints(run.trace.map { it.toEntity(run.id) })
        }
    }

    /** Edit a run's notes/title in place (no trace rewrite). */
    suspend fun updateRunNotes(id: String, notes: String) = dao.updateNotes(id, notes)

    /** Cascade removes the trace. */
    suspend fun deleteRun(id: String) = dao.deleteRun(id)

    // --- Routes --------------------------------------------------------------------------------

    fun observeRoutes(): Flow<List<Route>> =
        dao.observeRoutes().map { rows -> rows.map { it.toDomain() } }

    suspend fun getRoute(id: String): Route? {
        val row = dao.getRoute(id) ?: return null
        val points = dao.getRoutePoints(id).map { it.toDomain() }
        return row.toDomain(points)
    }

    suspend fun saveRoute(route: Route) {
        val encoded = if (route.points.isNotEmpty()) Polyline.encodeRoute(route.points) else route.encodedPolyline
        dao.upsertRoute(route.copy(encodedPolyline = encoded).toEntity())
        if (route.points.isNotEmpty()) {
            dao.upsertRoutePoints(route.points.map { it.toEntity(route.id) })
        }
    }

    suspend fun deleteRoute(id: String) = dao.deleteRoute(id)

    // --- Export/import (ExportBundle 1.5) ------------------------------------------------------

    /** All runs with traces, for a portable export snapshot. */
    suspend fun exportRuns(): List<Run> = dao.getAllRuns().map { row ->
        row.toDomain(dao.getRunPoints(row.id).map { it.toDomain() })
    }

    /** All routes with points, for a portable export snapshot. */
    suspend fun exportRoutes(): List<Route> = dao.getAllRoutes().map { row ->
        row.toDomain(dao.getRoutePoints(row.id).map { it.toDomain() })
    }
}

// --- Entity <-> domain mapping -----------------------------------------------------------------

private fun RunEntity.toDomain(trace: List<TracePoint> = emptyList()) = Run(
    id = id,
    sessionId = sessionId,
    startedAtUtc = startedAtUtc,
    durationSeconds = durationSeconds,
    movingSeconds = movingSeconds,
    distanceMeters = distanceMeters,
    avgPaceSecPerKm = avgPaceSecPerKm,
    elevationGainM = elevationGainM,
    calories = calories,
    avgHr = avgHr,
    source = source,
    externalKey = externalKey,
    encodedPolyline = encodedPolyline,
    routeId = routeId,
    notes = notes,
    trace = trace,
)

private fun Run.toEntity() = RunEntity(
    id = id,
    sessionId = sessionId,
    startedAtUtc = startedAtUtc,
    durationSeconds = durationSeconds,
    movingSeconds = movingSeconds,
    distanceMeters = distanceMeters,
    avgPaceSecPerKm = avgPaceSecPerKm,
    elevationGainM = elevationGainM,
    calories = calories,
    avgHr = avgHr,
    source = source,
    externalKey = externalKey,
    encodedPolyline = encodedPolyline,
    routeId = routeId,
    notes = notes,
)

private fun RunPointEntity.toDomain() = TracePoint(
    t = t, lat = lat, lon = lon, elevationM = elevationM,
    speedMps = speedMps, hrBpm = hrBpm, accuracyM = accuracyM,
)

private fun TracePoint.toEntity(runId: String) = RunPointEntity(
    runId = runId, t = t, lat = lat, lon = lon, elevationM = elevationM,
    speedMps = speedMps, hrBpm = hrBpm, accuracyM = accuracyM,
)

private fun RouteEntity.toDomain(points: List<RoutePoint> = emptyList()) = Route(
    id = id,
    name = name,
    distanceMeters = distanceMeters,
    elevationGainM = elevationGainM,
    encodedPolyline = encodedPolyline,
    createdAtUtc = createdAtUtc,
    notes = notes,
    points = points,
)

private fun Route.toEntity() = RouteEntity(
    id = id,
    name = name,
    distanceMeters = distanceMeters,
    elevationGainM = elevationGainM,
    encodedPolyline = encodedPolyline,
    createdAtUtc = createdAtUtc,
    notes = notes,
)

private fun RoutePointEntity.toDomain() = RoutePoint(seq = seq, lat = lat, lon = lon, elevationM = elevationM)

private fun RoutePoint.toEntity(routeId: String) =
    RoutePointEntity(routeId = routeId, seq = seq, lat = lat, lon = lon, elevationM = elevationM)
