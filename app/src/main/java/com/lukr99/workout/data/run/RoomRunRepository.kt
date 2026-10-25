package com.lukr99.workout.data.run

import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The Room-backed [RunRepository]: the **only** class that touches Room for Run Mode. It maps the
 * Room `@Entity` shapes to/from the portable `domain/run` model and owns the polyline encode/decode.
 */
class RoomRunRepository(private val dao: RunDao) : RunRepository {

    // --- Runs ----------------------------------------------------------------------------------

    override fun observeRuns(): Flow<List<Run>> = dao.observeRuns().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRuns(): List<Run> = dao.getAllRuns().map { it.toDomain() }

    override suspend fun countRuns(): Int = dao.countRuns()

    override suspend fun getRunsWithTraces(): List<Run> = dao.getAllRuns().map { row ->
        row.toDomain(dao.getRunPoints(row.id).map { it.toDomain() })
    }

    override suspend fun getRun(id: String): Run? {
        val row = dao.getRun(id) ?: return null
        val trace = dao.getRunPoints(id).map { it.toDomain() }
        return row.toDomain(trace)
    }

    override suspend fun saveRun(run: Run) {
        val encoded = if (run.trace.isNotEmpty()) Polyline.encodeTrace(run.trace) else run.encodedPolyline
        dao.upsertRun(run.copy(encodedPolyline = encoded).toEntity())
        if (run.trace.isNotEmpty()) {
            dao.upsertRunPoints(run.trace.map { it.toEntity(run.id) })
        }
    }

    override suspend fun updateRunNotes(id: String, notes: String) = dao.updateNotes(id, notes)

    override suspend fun deleteRun(id: String) = dao.deleteRun(id)

    // --- Routes --------------------------------------------------------------------------------

    override fun observeRoutes(): Flow<List<Route>> =
        dao.observeRoutes().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRoutes(): List<Route> = dao.getAllRoutes().map { it.toDomain() }

    override suspend fun getRoute(id: String): Route? {
        val row = dao.getRoute(id) ?: return null
        val points = dao.getRoutePoints(id).map { it.toDomain() }
        return row.toDomain(points)
    }

    override suspend fun saveRoute(route: Route) {
        val encoded = if (route.points.isNotEmpty()) Polyline.encodeRoute(route.points) else route.encodedPolyline
        dao.upsertRoute(route.copy(encodedPolyline = encoded).toEntity())
        if (route.points.isNotEmpty()) {
            dao.upsertRoutePoints(route.points.map { it.toEntity(route.id) })
        }
    }

    override suspend fun deleteRoute(id: String) = dao.deleteRoute(id)

    override suspend fun countRoutes(): Int = dao.countRoutes()

    override suspend fun deleteAll() {
        dao.deleteAllRuns()
        dao.deleteAllRoutes()
    }

    // --- Export/import (ExportBundle 1.5) ------------------------------------------------------

    override suspend fun exportRuns(): List<Run> = dao.getAllRuns().map { row ->
        row.toDomain(dao.getRunPoints(row.id).map { it.toDomain() })
    }

    override suspend fun exportRoutes(): List<Route> = dao.getAllRoutes().map { row ->
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
    speedMps = speedMps, hrBpm = hrBpm, accuracyM = accuracyM, segmentStart = segmentStart,
)

private fun TracePoint.toEntity(runId: String) = RunPointEntity(
    runId = runId, t = t, lat = lat, lon = lon, elevationM = elevationM,
    speedMps = speedMps, hrBpm = hrBpm, accuracyM = accuracyM, segmentStart = segmentStart,
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
