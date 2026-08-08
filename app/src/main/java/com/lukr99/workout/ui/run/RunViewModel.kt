package com.lukr99.workout.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.run.RunRepository
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
class RunViewModel(private val repo: RunRepository) : ViewModel() {

    val runs: StateFlow<List<Run>> =
        repo.observeRuns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<Route>> =
        repo.observeRoutes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RunViewModel(container.runRepository) as T
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
