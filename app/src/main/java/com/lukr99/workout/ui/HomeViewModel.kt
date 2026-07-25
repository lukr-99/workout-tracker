package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.DashboardSnapshot
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home dashboard state — the resume card, quick-start templates, and recent sessions. */
class HomeViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val snapshotState = MutableStateFlow(DashboardSnapshot())
    val snapshot: StateFlow<DashboardSnapshot> = snapshotState.asStateFlow()

    val activeSession: StateFlow<WorkoutSession?> =
        repo.observeActiveSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val templates: StateFlow<List<WorkoutTemplate>> =
        repo.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Recompute the dashboard whenever history or the active session changes.
        repo.observeHistory().onEach { refresh() }.launchIn(viewModelScope)
        activeSession.onEach { refresh() }.launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch { snapshotState.value = repo.getDashboardSnapshot() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container.repository) as T
        }
    }
}
