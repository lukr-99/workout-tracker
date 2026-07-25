package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.domain.WorkoutTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The screens' single entry point to state and actions (MVVM, unidirectional data flow). Backed by
 * the real [WorkoutRepository] as of Phase 1 — the catalog, templates, active session, and history
 * are live Room `Flow`s surfaced as `StateFlow`. Richer per-area state and the logging actions land
 * with the Phase 2 screens; this proves the data core is wired end-to-end.
 */
class WorkoutViewModel(private val repo: WorkoutRepository) : ViewModel() {

    val exercises: StateFlow<List<Exercise>> =
        repo.observeExercises().stateInDefault(emptyList())

    val templates: StateFlow<List<WorkoutTemplate>> =
        repo.observeTemplates().stateInDefault(emptyList())

    val activeSession: StateFlow<WorkoutSession?> =
        repo.observeActiveSession().stateInDefault(null)

    val history: StateFlow<List<WorkoutSessionSummary>> =
        repo.observeHistory().stateInDefault(emptyList())

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInDefault(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    companion object {
        /** Wired in `MainActivity` from `AppContainer.repository`. */
        fun factory(repo: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkoutViewModel(repo) as T
            }
    }
}
