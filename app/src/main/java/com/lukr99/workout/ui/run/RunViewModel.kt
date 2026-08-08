package com.lukr99.workout.ui.run

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.map.OfflineTileCache
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.run.ShareCardRenderer
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.ExportArtifact
import com.lukr99.workout.domain.run.GpxCodec
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.RunStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Run Mode hub + detail + stats state. Observes the v5 tables for the [RunsScreen] list, loads a
 * single run (with trace) for [RunDetailScreen], derives the running [RunningStats] shown in the
 * Progress tab, and owns edit/delete. Recording itself lives in [LiveRunViewModel].
 */
class RunViewModel(
    private val repo: RunRepository,
    private val documents: AndroidDocumentGateway,
    private val shareCards: ShareCardRenderer,
    private val offlineCache: OfflineTileCache,
) : ViewModel() {

    val runs: StateFlow<List<Run>> =
        repo.observeRuns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<Route>> =
        repo.observeRoutes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Transient user-facing status for GPX import + offline caching (banner/snackbar), or null. */
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()
    fun clearStatus() { _status.value = null }

    private val _stats = MutableStateFlow(RunningStats())
    val stats: StateFlow<RunningStats> = _stats.asStateFlow()

    private val _detail = MutableStateFlow<Run?>(null)
    val detail: StateFlow<Run?> = _detail.asStateFlow()

    private val _detailRouteName = MutableStateFlow<String?>(null)
    /** Name of the saved route a run was started from (a reference mention only), or null. */
    val detailRouteName: StateFlow<String?> = _detailRouteName.asStateFlow()

    init {
        // Recompute the Progress running stats whenever the run set changes (loads traces for PRs).
        viewModelScope.launch {
            repo.observeRuns().collect { refreshStats() }
        }
    }

    private suspend fun refreshStats() {
        val detailed = repo.getRunsWithTraces()
        _stats.value = RunningStats(
            totals = RunStats.totals(detailed),
            weekly = RunStats.weeklyDistance(detailed),
            paceTrend = RunStats.paceTrend(detailed),
            streakWeeks = RunStats.currentStreakWeeks(detailed),
            records = RunStats.personalRecords(detailed),
        )
    }

    /** Load a run with its trace for the detail screen (plus its linked route name, if any). */
    fun openRun(id: String) {
        viewModelScope.launch {
            val run = repo.getRun(id)
            _detail.value = run
            _detailRouteName.value = run?.routeId?.let { repo.getRoute(it)?.name?.ifBlank { "Route" } }
        }
    }

    fun clearDetail() {
        _detail.value = null
        _detailRouteName.value = null
    }

    fun updateNotes(id: String, notes: String) {
        viewModelScope.launch {
            repo.updateRunNotes(id, notes)
            _detail.value = _detail.value?.takeIf { it.id == id }?.copy(notes = notes)
        }
    }

    fun deleteRun(id: String) {
        viewModelScope.launch {
            repo.deleteRun(id)
            if (_detail.value?.id == id) _detail.value = null
        }
    }

    // --- Route management (R3 deferred slice) --------------------------------------------------

    fun renameRoute(route: Route, name: String) {
        viewModelScope.launch { repo.saveRoute(route.copy(name = name.trim())) }
    }

    fun deleteRoute(id: String) {
        viewModelScope.launch { repo.deleteRoute(id) }
    }

    // --- GPX import / export (R5) --------------------------------------------------------------

    /** Build a share intent carrying a run's trace as a GPX 1.1 file. Loads the trace first. */
    suspend fun gpxShareIntent(runId: String): Intent {
        val run = repo.getRun(runId) ?: error("Run not found")
        val name = run.notes.ifBlank { "run" }.take(40)
        val artifact = ExportArtifact(
            fileName = safeFileName(name) + "." + GpxCodec.EXTENSION,
            mimeType = GpxCodec.MIME_TYPE,
            format = com.lukr99.workout.data.transfer.DataFormat.WorkoutJson, // format field unused for GPX
            text = GpxCodec.encode(run, name),
            recordCount = run.trace.size,
        )
        return documents.shareIntent(artifact)
    }

    /** Import a GPX file at [uri] into a saved run (source Imported). Surfaces a status message. */
    fun importGpx(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = documents.readText(uri)
                val run = GpxCodec.decode(text) ?: error("No track points found")
                repo.saveRun(run)
                run
            }
            _status.value = result.fold(
                onSuccess = { "Imported run · ${(it.distanceMeters / 1000).format1()} km" },
                onFailure = { "Couldn't import GPX: ${it.message}" },
            )
        }
    }

    // --- Share card (R5) -----------------------------------------------------------------------

    suspend fun shareCardIntent(runId: String, imperial: Boolean): Intent {
        val run = repo.getRun(runId) ?: error("Run not found")
        return shareCards.shareIntent(run, imperial)
    }

    // --- Offline tiles (R5; closes deferred R3 slice) ------------------------------------------

    /** Cache the map region around a saved route for offline use. Progress → [status]. */
    fun downloadRouteOffline(route: Route) {
        viewModelScope.launch {
            val full = repo.getRoute(route.id) ?: route
            val path = full.points.takeIf { it.isNotEmpty() }?.map { it.lat to it.lon }
                ?: Polyline.decode(full.encodedPolyline)
            val bounds = OfflineTileCache.boundsForPath(path)
            if (bounds == null) {
                _status.value = "This route has no path to cache."
                return@launch
            }
            _status.value = "Caching map for offline…"
            offlineCache.download(bounds, full.name.ifBlank { "Route" }, object : OfflineTileCache.Listener {
                override fun onProgress(percent: Int) { _status.value = "Caching map… $percent%" }
                override fun onComplete() { _status.value = "Map cached for offline use." }
                override fun onError(reason: String) { _status.value = "Offline caching failed: $reason" }
            })
        }
    }

    private fun Double.format1(): String = "%.1f".format(this)

    private fun safeFileName(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._ -]"), "_").ifBlank { "run" }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RunViewModel(
                    container.runRepository,
                    container.documents,
                    container.shareCardRenderer,
                    container.offlineTileCache,
                ) as T
        }
    }
}

/** Derived running analytics for the Progress tab (empty until the first run is recorded). */
data class RunningStats(
    val totals: RunStats.Totals = RunStats.Totals(0, 0.0, 0, 0.0, 0.0),
    val weekly: List<RunStats.PeriodBucket> = emptyList(),
    val paceTrend: List<Pair<Long, Double>> = emptyList(),
    val streakWeeks: Int = 0,
    val records: RunStats.PersonalRecords = RunStats.PersonalRecords(),
) {
    val hasRuns: Boolean get() = totals.runCount > 0
}
