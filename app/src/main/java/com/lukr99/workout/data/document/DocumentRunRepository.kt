package com.lukr99.workout.data.document

import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The [RunRepository] over a [DocumentDatabase]. The web edition has no Run Mode, but runs and
 * routes from an imported Android backup must survive a web backup, so they are stored as they
 * arrived (the encoded polyline is not recomputed) and written back unchanged. Writes join the
 * workout repository's transactions, so a restore stays all or nothing.
 */
class DocumentRunRepository(private val database: DocumentDatabase) : RunRepository {

    private suspend fun runs(): List<Run> = database.current().data.runs.sortedByDescending { it.startedAtUtc }

    private suspend fun routes(): List<Route> = database.current().data.routes.sortedByDescending { it.createdAtUtc }

    // --- Runs ----------------------------------------------------------------------------------

    override fun observeRuns(): Flow<List<Run>> = database.document
        .map { document -> document.data.runs.sortedByDescending { it.startedAtUtc }.map { it.copy(trace = emptyList()) } }
        .distinctUntilChanged()

    override suspend fun getRuns(): List<Run> = runs().map { it.copy(trace = emptyList()) }

    override suspend fun countRuns(): Int = database.current().data.runs.size

    override suspend fun getRunsWithTraces(): List<Run> = runs()

    override suspend fun getRun(id: String): Run? = database.current().data.runs.firstOrNull { it.id == id }

    override suspend fun saveRun(run: Run) = database.update { document ->
        document.edit {
            val existing = runs.firstOrNull { it.id == run.id }
            // As in Room, a run saved without its trace keeps the trace already stored.
            val saved = if (run.trace.isEmpty() && existing != null) run.copy(trace = existing.trace) else run
            copy(runs = runs.upsert(saved) { it.id == run.id })
        }
    }

    override suspend fun updateRunNotes(id: String, notes: String) = database.update { document ->
        document.edit { copy(runs = runs.map { if (it.id == id) it.copy(notes = notes) else it }) }
    }

    override suspend fun deleteRun(id: String) = database.update { document ->
        document.edit { copy(runs = runs.filterNot { it.id == id }) }
    }

    // --- Routes --------------------------------------------------------------------------------

    override fun observeRoutes(): Flow<List<Route>> = database.document
        .map { document -> document.data.routes.sortedByDescending { it.createdAtUtc }.map { it.copy(points = emptyList()) } }
        .distinctUntilChanged()

    override suspend fun getRoutes(): List<Route> = routes().map { it.copy(points = emptyList()) }

    override suspend fun getRoute(id: String): Route? = database.current().data.routes.firstOrNull { it.id == id }

    override suspend fun saveRoute(route: Route) = database.update { document ->
        document.edit {
            val existing = routes.firstOrNull { it.id == route.id }
            val saved = if (route.points.isEmpty() && existing != null) route.copy(points = existing.points) else route
            copy(routes = routes.upsert(saved) { it.id == route.id })
        }
    }

    override suspend fun deleteRoute(id: String) = database.update { document ->
        document.edit { copy(routes = routes.filterNot { it.id == id }) }
    }

    override suspend fun countRoutes(): Int = database.current().data.routes.size

    override suspend fun deleteAll() = database.update { document ->
        document.edit { copy(runs = emptyList(), routes = emptyList()) }
    }

    // --- Export ----------------------------------------------------------------------------------

    override suspend fun exportRuns(): List<Run> = runs()

    override suspend fun exportRoutes(): List<Route> = routes()
}
