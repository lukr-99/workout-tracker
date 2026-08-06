package com.lukr99.workout.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Run Mode hub state — recent runs + saved routes for [com.lukr99.workout.ui.run.RunsScreen].
 *
 * R0 is read-only foundation: it observes the v5 tables (empty on a fresh upgrade) so the hub can
 * show its empty state. Recording/persisting a run arrives in R1 ([RunRepository.saveRun]).
 */
class RunViewModel(private val repo: RunRepository) : ViewModel() {

    val runs: StateFlow<List<Run>> =
        repo.observeRuns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<Route>> =
        repo.observeRoutes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RunViewModel(container.runRepository) as T
        }
    }
}
