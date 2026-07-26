package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.creation.CreationResult
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.domain.creation.TemplateDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Library surface state — the exercise catalog (searchable/filterable) and workout templates. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(
    private val repo: WorkoutRepository,
    private val data: WorkoutDataService,
) : ViewModel() {

    private val filterState = MutableStateFlow(ExerciseFilter())
    val filter: StateFlow<ExerciseFilter> = filterState.asStateFlow()

    val exercises: StateFlow<List<Exercise>> =
        filterState.debounce(250).flatMapLatest { repo.observeExercises(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<WorkoutTemplate>> =
        repo.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(text: String) = filterState.update { it.copy(searchText = text) }
    fun setBodyPart(part: String) = filterState.update { it.copy(bodyPart = part) }
    fun setCategory(category: ExerciseCategory?) = filterState.update { it.copy(category = category) }
    fun setEquipment(equipment: String) = filterState.update { it.copy(equipment = equipment) }
    fun setIncludeArchived(include: Boolean) = filterState.update { it.copy(includeArchived = include) }

    fun saveExercise(draft: ExerciseDraft, onResult: (CreationResult<Exercise>) -> Unit = {}) {
        viewModelScope.launch { onResult(data.createExercise(draft)) }
    }

    fun archiveExercise(id: String) = viewModelScope.launch { repo.archiveExercise(id) }.let { }

    fun restoreExercise(exercise: Exercise) =
        viewModelScope.launch { repo.saveExercise(exercise.copy(isArchived = false)) }.let { }

    fun saveTemplate(draft: TemplateDraft, onResult: (CreationResult<WorkoutTemplate>) -> Unit = {}) {
        viewModelScope.launch { onResult(data.createTemplate(draft)) }
    }

    fun deleteTemplate(id: String) = viewModelScope.launch { repo.deleteTemplate(id) }.let { }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LibraryViewModel(container.repository, container.workoutData) as T
        }
    }
}
