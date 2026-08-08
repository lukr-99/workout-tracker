package com.lukr99.workout.data.run

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Room query/mutation surface for runs + routes. Flow reads drive the reactive UI; writes are
 * upserts; deletes rely on `ForeignKey.CASCADE` for child points. Only [RunRepository] calls this.
 */
@Dao
interface RunDao {

    // --- Runs ----------------------------------------------------------------------------------

    @Query("SELECT * FROM runs ORDER BY startedAtUtc DESC")
    fun observeRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs ORDER BY startedAtUtc DESC")
    suspend fun getAllRuns(): List<RunEntity>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRun(id: String): RunEntity?

    @Query("SELECT COUNT(*) FROM runs")
    suspend fun countRuns(): Int

    @Upsert
    suspend fun upsertRun(run: RunEntity)

    /** Edit just the notes/title without rewriting the trace. */
    @Query("UPDATE runs SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)

    @Query("SELECT * FROM run_points WHERE runId = :runId ORDER BY t")
    suspend fun getRunPoints(runId: String): List<RunPointEntity>

    @Upsert
    suspend fun upsertRunPoints(points: List<RunPointEntity>)

    /** Cascade removes `run_points`. */
    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRun(id: String)

    // --- Routes --------------------------------------------------------------------------------

    @Query("SELECT * FROM routes ORDER BY createdAtUtc DESC")
    fun observeRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes ORDER BY createdAtUtc DESC")
    suspend fun getAllRoutes(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRoute(id: String): RouteEntity?

    @Upsert
    suspend fun upsertRoute(route: RouteEntity)

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY seq")
    suspend fun getRoutePoints(routeId: String): List<RoutePointEntity>

    @Upsert
    suspend fun upsertRoutePoints(points: List<RoutePointEntity>)

    /** Cascade removes `route_points`. */
    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRoute(id: String)
}
