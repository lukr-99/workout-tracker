package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Past-session list (searchable) plus the drill-in detail with edit-after-the-fact. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val searchState = MutableStateFlow("")
    val search: StateFlow<String> = searchState.asStateFlow()

    val history: StateFlow<List<WorkoutSessionSummary>> =
        searchState.flatMapLatest { repo.observeHistory(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedState = MutableStateFlow<WorkoutSession?>(null)
    val selected: StateFlow<WorkoutSession?> = selectedState.asStateFlow()

    fun setSearch(text: String) { searchState.value = text }

    fun open(id: String) {
        viewModelScope.launch { selectedState.value = repo.getSession(id) }
    }

    fun clearSelection() { selectedState.value = null }

    fun save(session: WorkoutSession, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val saved = repo.saveWorkoutSession(session)
            selectedState.value = saved
            onDone()
        }
    }

    fun delete(id: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteWorkoutSession(id)
            selectedState.value = null
            onDone()
        }
    }

    fun makeTemplate(sessionId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.duplicateWorkoutAsTemplate(sessionId)
            onDone()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(container.repository) as T
        }
    }
}
