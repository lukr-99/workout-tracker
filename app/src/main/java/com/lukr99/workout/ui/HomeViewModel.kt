package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.DashboardSnapshot
import com.lukr99.workout.domain.TrainingWeek
import com.lukr99.workout.domain.WhatsNewNote
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.WhatsNewGate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Home: the resume card, this week's days and totals, templates to start from, and recent activity. */
class HomeViewModel(
    private val repo: WorkoutRepository,
    runs: RunRepository,
    private val whatsNewGate: WhatsNewGate? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    private val snapshotState = MutableStateFlow(DashboardSnapshot())
    val snapshot: StateFlow<DashboardSnapshot> = snapshotState.asStateFlow()

    val activeSession: StateFlow<WorkoutSession?> =
        repo.observeActiveSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val templates: StateFlow<List<WorkoutTemplate>> =
        repo.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The "what's new" card for this build, or null when there is nothing to show. */
    val whatsNew: StateFlow<WhatsNewNote?> = (whatsNewGate?.due ?: flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissWhatsNew() {
        viewModelScope.launch { whatsNewGate?.dismiss() }
    }

    private val history = repo.observeHistory()
    private val runList = runs.observeRuns()

    /** When each template was last finished, by template id; templates never done are left out. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val templateLastDone: StateFlow<Map<String, Long>> =
        combine(templates, history) { list, _ -> list }
            .mapLatest { repo.getTemplatesLastDone() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val week: StateFlow<TrainingWeek> =
        combine(history, runList) { workouts, runs -> TrainingWeek.of(today(), zone(), workouts, runs) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainingWeek.of(today(), zone(), emptyList(), emptyList()))

    val recent: StateFlow<List<HomeRecent>> =
        combine(history, runList) { workouts, runs ->
            val lifts = workouts.map {
                HomeRecent(it.id, isRun = false, name = it.name, atUtc = it.completedDateUtc ?: it.startedAtUtc, amount = it.totalVolumeKg)
            }
            val runRows = runs.map {
                HomeRecent(it.id, isRun = true, name = it.notes.lineSequence().firstOrNull()?.takeIf(String::isNotBlank) ?: "Run", atUtc = it.startedAtUtc, amount = it.distanceMeters)
            }
            (lifts + runRows).sortedByDescending(HomeRecent::atUtc).take(RECENT_ROWS)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // The streak and the resume card come from the dashboard, recomputed when history or the
        // live workout changes.
        history.onEach { refresh() }.launchIn(viewModelScope)
        activeSession.onEach { refresh() }.launchIn(viewModelScope)
    }

    suspend fun preview(template: WorkoutTemplate): TemplatePreviewData = TemplatePreviewData(
        lastSets = repo.getLastSets(template.exercises.map { it.exerciseId }),
        lastDoneUtc = repo.getTemplateLastDone(template.id),
    )

    fun refresh() {
        viewModelScope.launch { snapshotState.value = repo.getDashboardSnapshot() }
    }

    private fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate()

    companion object {
        const val RECENT_ROWS = 5

        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container.repository, container.runRepository, container.whatsNew) as T
        }
    }
}
