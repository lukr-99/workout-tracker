package com.lukr99.workout.data.run

import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import kotlinx.coroutines.flow.Flow

/**
 * The single API over run and route persistence, the way ui and services reach Run Mode data
 * (mirrors [com.lukr99.workout.data.WorkoutRepository] for strength). [RoomRunRepository] is the
 * Room implementation. The model is the portable `domain/run` one.
 *
 * R0 scope: read/write skeleton + list flows. Live-run persistence (incremental point writes,
 * moving-time/split derivation) arrives in R1; Health Connect export in R2.
 */
interface RunRepository {

    /** Recent-first run summaries (no trace loaded — the encoded polyline covers thumbnails). */
    fun observeRuns(): Flow<List<Run>>

    suspend fun getRuns(): List<Run>

    suspend fun countRuns(): Int

    /** All runs with their full traces loaded — for stats/PRs that need sub-run windows. */
    suspend fun getRunsWithTraces(): List<Run>

    /** A run with its full raw trace loaded — for detail, editing, or GPX export. */
    suspend fun getRun(id: String): Run?

    /**
     * Persist a run and (re)write its raw trace. The `encodedPolyline` is derived from [run.trace]
     * when present so the stored fast-path always matches the points.
     */
    suspend fun saveRun(run: Run)

    /** Edit a run's notes/title in place (no trace rewrite). */
    suspend fun updateRunNotes(id: String, notes: String)

    /** Cascade removes the trace. */
    suspend fun deleteRun(id: String)

    fun observeRoutes(): Flow<List<Route>>

    /** Saved route summaries (no points loaded), newest first. */
    suspend fun getRoutes(): List<Route>

    suspend fun getRoute(id: String): Route?

    suspend fun saveRoute(route: Route)

    suspend fun deleteRoute(id: String)

    suspend fun countRoutes(): Int

    /** Removes every run and route with their points. Callers wrap it in the restore transaction. */
    suspend fun deleteAll()

    /** All runs with traces, for a portable export snapshot. */
    suspend fun exportRuns(): List<Run>

    /** All routes with points, for a portable export snapshot. */
    suspend fun exportRoutes(): List<Route>

}
