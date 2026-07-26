package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.newId
import com.lukr99.workout.domain.progression.DoubleProgression
import com.lukr99.workout.domain.progression.SuggestionStatus
import com.lukr99.workout.domain.records.RecordKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The live logging loop (Phase 2's highest-priority screen). Holds an in-memory working draft of the
 * active session so steppers stay responsive; flushes to the repository on structural changes, on
 * marking a set done, and on finish/discard — so a live session survives app restarts. All data
 * rules stay in the repository; this only orchestrates edits and the rest timer.
 */
class LiveWorkoutViewModel(
    private val repo: WorkoutRepository,
    private val settings: com.lukr99.workout.settings.SettingsStore,
    private val insights: WorkoutInsightsService,
) : ViewModel() {

    /** Live snapshot of the user's default rest, refreshed from DataStore. */
    private var defaultRest = 120
    init {
        viewModelScope.launch { settings.settings.collect { defaultRest = it.defaultRestSeconds } }
    }

    private val draftState = MutableStateFlow<WorkoutSession?>(null)
    val draft: StateFlow<WorkoutSession?> = draftState.asStateFlow()

    private val doneSetIdsState = MutableStateFlow<Set<String>>(emptySet())
    val doneSetIds: StateFlow<Set<String>> = doneSetIdsState.asStateFlow()

    private val restState = MutableStateFlow(RestState())
    val rest: StateFlow<RestState> = restState.asStateFlow()

    /** Fires when a just-completed set beats a stored record; the screen renders the PR treatment. */
    private val prEventState = MutableStateFlow<PrEvent?>(null)
    val prEvent: StateFlow<PrEvent?> = prEventState.asStateFlow()
    fun consumePrEvent() { prEventState.value = null }

    /** A one-shot progression rationale to surface (toast) after an exercise is added with a suggestion. */
    private val suggestionState = MutableStateFlow<String?>(null)
    val suggestion: StateFlow<String?> = suggestionState.asStateFlow()
    fun consumeSuggestion() { suggestionState.value = null }

    /** Non-archived catalog for the add-exercise picker. */
    val exercises: StateFlow<List<Exercise>> =
        repo.observeExercises().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var restJob: Job? = null

    /** Ensure a live session exists (resumes an existing one) and load it into the draft. */
    fun startOrResume(templateId: String? = null) {
        viewModelScope.launch {
            val session = repo.createWorkoutSession(templateId = templateId)
            draftState.value = session
            doneSetIdsState.value = emptySet()
        }
    }

    fun loadActiveIfAny() {
        if (draftState.value != null) return
        viewModelScope.launch { repo.getActiveSession()?.let { draftState.value = it } }
    }

    // --- Structural edits ----------------------------------------------------------------------

    fun rename(name: String) = mutate(persist = false) { it.copy(name = name) }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            val base = repo.newEntryForExercise(exercise, sortOrder = draftState.value?.entries?.size ?: 0)
            val entry = if (exercise.category == ExerciseCategory.Strength) {
                val suggested = suggestedSets(exercise)
                when {
                    suggested != null -> base.copy(strengthSets = suggested)
                    else -> {
                        val prefill = lastPerformance(exercise.id)
                        if (prefill.isNotEmpty()) base.copy(strengthSets = prefill) else base
                    }
                }
            } else base
            mutate { it.copy(entries = it.entries + entry.copy(workoutSessionId = it.id)) }
        }
    }

    /**
     * Phase 3.5 `insights.progression` as the pre-filled next sets (default double-progression). Emits
     * the rationale for a toast. Returns null when there is not enough history to suggest.
     */
    private suspend fun suggestedSets(exercise: Exercise): List<StrengthSet>? {
        if (exercise.id.isBlank()) return null
        val suggestion = runCatching { insights.progression(exercise.id, DoubleProgression()) }.getOrNull()
            ?: return null
        if (suggestion.status != SuggestionStatus.Ready || suggestion.targets.isEmpty()) return null
        suggestionState.value = suggestion.rationale
        return suggestion.targets.mapIndexed { i, t ->
            StrengthSet(
                id = newId(),
                setNumber = i + 1,
                reps = t.reps,
                weightKg = t.weightKg,
                setType = t.setType,
            )
        }
    }

    fun removeEntry(entryId: String) = mutate { session ->
        session.copy(entries = session.entries.filterNot { it.id == entryId })
    }

    fun moveEntry(entryId: String, up: Boolean) = mutate { session ->
        val list = session.entries.toMutableList()
        val i = list.indexOfFirst { it.id == entryId }
        if (i < 0) return@mutate session
        val j = if (up) i - 1 else i + 1
        if (j !in list.indices) return@mutate session
        list.add(j, list.removeAt(i))
        session.copy(entries = list.mapIndexed { index, e -> e.copy(sortOrder = index) })
    }

    fun addSet(entryId: String) = mutate { session ->
        session.copy(entries = session.entries.map { entry ->
            if (entry.id != entryId) return@map entry
            val template = entry.strengthSets.lastOrNull()
            val next = StrengthSet(
                id = newId(),
                workoutEntryId = entryId,
                setNumber = entry.strengthSets.size + 1,
                reps = template?.reps ?: 0,
                weightKg = template?.weightKg ?: 0.0,
                setType = SetType.Normal,
            )
            entry.copy(strengthSets = entry.strengthSets + next)
        })
    }

    fun removeSet(entryId: String, setId: String) = mutate { session ->
        session.copy(entries = session.entries.map { entry ->
            if (entry.id != entryId) entry
            else entry.copy(strengthSets = entry.strengthSets.filterNot { it.id == setId })
        })
    }

    // --- Set field edits (in-memory only; flushed on done/finish) -------------------------------

    fun updateSet(entryId: String, setId: String, transform: (StrengthSet) -> StrengthSet) =
        mutate(persist = false) { session ->
            session.copy(entries = session.entries.map { entry ->
                if (entry.id != entryId) entry
                else entry.copy(strengthSets = entry.strengthSets.map { if (it.id == setId) transform(it) else it })
            })
        }

    fun setReps(entryId: String, setId: String, reps: Int) =
        updateSet(entryId, setId) { it.copy(reps = reps.coerceAtLeast(0)) }

    fun setWeight(entryId: String, setId: String, weightKg: Double) =
        updateSet(entryId, setId) { it.copy(weightKg = weightKg.coerceAtLeast(0.0)) }

    fun setRir(entryId: String, setId: String, rir: Double?) =
        updateSet(entryId, setId) { it.copy(rir = rir) }

    fun setRpe(entryId: String, setId: String, rpe: Double?) =
        updateSet(entryId, setId) { it.copy(rpe = rpe) }

    /** Edit a cardio entry's duration/distance/calories in the live draft (persisted on finish). */
    fun updateCardio(entryId: String, transform: (com.lukr99.workout.domain.CardioEntryData) -> com.lukr99.workout.domain.CardioEntryData) =
        mutate(persist = false) { session ->
            session.copy(entries = session.entries.map { entry ->
                if (entry.id != entryId) entry
                else entry.copy(cardioData = transform(entry.cardioData ?: com.lukr99.workout.domain.CardioEntryData(workoutEntryId = entry.id)))
            })
        }

    fun setType(entryId: String, setId: String, type: SetType) = mutate {
        it.copy(entries = it.entries.map { entry ->
            if (entry.id != entryId) entry
            else entry.copy(strengthSets = entry.strengthSets.map { s ->
                if (s.id == setId) s.copy(setType = type, isWarmup = type == SetType.Warmup) else s
            })
        })
    }

    /** Mark/unmark a set done. Marking done persists the draft and starts the rest timer. */
    fun toggleSetDone(entryId: String, setId: String) {
        val currentlyDone = setId in doneSetIdsState.value
        doneSetIdsState.update { if (currentlyDone) it - setId else it + setId }
        if (!currentlyDone) {
            val entry = draftState.value?.entries?.firstOrNull { it.id == entryId }
            val restSecs = entry?.let { restSecondsFor(it) } ?: defaultRest
            startRest(restSecs)
            persist()
            evaluatePr(entryId, setId)
        }
    }

    /**
     * On set-done, ask Phase 3.5 `insights.evaluateSetRecord` whether the entered set beats a stored
     * record. If so, flag the set as a PR (persisted, so it shows the PR badge everywhere) and emit a
     * [PrEvent] the screen turns into the count-up + glow + haptic.
     */
    private fun evaluatePr(entryId: String, setId: String) {
        val entry = draftState.value?.entries?.firstOrNull { it.id == entryId } ?: return
        val set = entry.strengthSets.firstOrNull { it.id == setId } ?: return
        if (entry.exerciseId.isBlank() || set.isWarmup || set.setType == SetType.Warmup) return
        if (set.reps <= 0 || set.weightKg <= 0.0) return
        viewModelScope.launch {
            val achievement = insights.evaluateSetRecord(entry.exerciseId, set)
            if (!achievement.isPersonalRecord) return@launch
            // Flag the set as a PR in the draft (persist so history keeps the badge).
            mutate {
                it.copy(entries = it.entries.map { e ->
                    if (e.id != entryId) e
                    else e.copy(strengthSets = e.strengthSets.map { s -> if (s.id == setId) s.copy(isPr = true) else s })
                })
            }
            prEventState.value = PrEvent(
                id = System.nanoTime(),
                exerciseName = entry.exerciseSnapshotName,
                estimated1RmKg = Estimates.epley(set.weightKg, set.reps),
                headline = achievement.headline(),
            )
        }
    }

    // --- Rest timer ----------------------------------------------------------------------------

    fun startRest(seconds: Int) {
        restJob?.cancel()
        restState.value = RestState(running = true, remaining = seconds, total = seconds)
        restJob = viewModelScope.launch {
            while (restState.value.running && restState.value.remaining > 0) {
                delay(1_000)
                restState.update { it.copy(remaining = (it.remaining - 1).coerceAtLeast(0)) }
            }
            if (restState.value.remaining <= 0) restState.update { it.copy(running = false) }
        }
    }

    fun addRest(seconds: Int) = restState.update {
        it.copy(remaining = (it.remaining + seconds).coerceAtLeast(0), total = maxOf(it.total, it.remaining + seconds))
    }

    fun skipRest() {
        restJob?.cancel()
        restState.value = RestState()
    }

    // --- Finish / discard ----------------------------------------------------------------------

    fun finish(onDone: () -> Unit) {
        val session = draftState.value ?: return onDone()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.saveWorkoutSession(
                session.copy(
                    status = WorkoutSessionStatus.Completed,
                    endedAtUtc = now,
                    completedDateUtc = now,
                    entries = session.entries.filter { it.strengthSets.isNotEmpty() || it.cardioData != null },
                ),
            )
            clear()
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        val session = draftState.value ?: return onDone()
        viewModelScope.launch {
            repo.saveWorkoutSession(session.copy(status = WorkoutSessionStatus.Discarded, endedAtUtc = System.currentTimeMillis()))
            clear()
            onDone()
        }
    }

    // --- Helpers -------------------------------------------------------------------------------

    fun estimatedVolumeKg(): Double =
        draftState.value?.entries?.sumOf { Estimates.volume(it.strengthSets) } ?: 0.0

    private fun restSecondsFor(entry: WorkoutEntry): Int {
        val fromCatalog = exercises.value.firstOrNull { it.id == entry.exerciseId }?.defaultRestSeconds
        return fromCatalog ?: defaultRest
    }

    private suspend fun lastPerformance(exerciseId: String): List<StrengthSet> {
        if (exerciseId.isBlank()) return emptyList()
        val session = repo.getSessions(includeDiscarded = false)
            .filter { it.status == WorkoutSessionStatus.Completed }
            .sortedByDescending { it.completedDateUtc ?: it.startedAtUtc }
            .firstOrNull { s -> s.entries.any { it.exerciseId == exerciseId && it.strengthSets.isNotEmpty() } }
            ?: return emptyList()
        val sets = session.entries.first { it.exerciseId == exerciseId && it.strengthSets.isNotEmpty() }.strengthSets
        return sets.mapIndexed { i, s ->
            StrengthSet(
                id = newId(),
                setNumber = i + 1,
                reps = s.reps,
                weightKg = s.weightKg,
                isWarmup = s.isWarmup,
                setType = s.setType,
            )
        }
    }

    private fun clear() {
        draftState.value = null
        doneSetIdsState.value = emptySet()
        skipRest()
    }

    private inline fun mutate(persist: Boolean = true, crossinline transform: (WorkoutSession) -> WorkoutSession) {
        val current = draftState.value ?: return
        val updated = transform(current)
        draftState.value = updated
        if (persist) persist()
    }

    /**
     * Fire-and-forget flush of the working draft. Ids are assigned client-side and the repository
     * preserves non-blank ids, so we deliberately do NOT read the result back — that would clobber
     * any reps/weight the user typed while the save was in flight.
     */
    private fun persist() {
        val session = draftState.value ?: return
        viewModelScope.launch { repo.saveWorkoutSession(session) }
    }

    data class RestState(
        val running: Boolean = false,
        val remaining: Int = 0,
        val total: Int = 0,
    )

    /** A new personal record achieved live; consumed by the screen after the celebration plays. */
    data class PrEvent(
        val id: Long,
        val exerciseName: String,
        val estimated1RmKg: Double,
        val headline: String,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LiveWorkoutViewModel(container.repository, container.settings, container.insights) as T
        }
    }
}

/** Human summary of the strongest achievement in a PR (e1RM beats heaviest beats volume). */
private fun com.lukr99.workout.domain.records.RecordAchievements.headline(): String = when {
    RecordKind.Estimated1Rm in kinds -> "New estimated 1RM"
    RecordKind.HeaviestSet in kinds -> "Heaviest set ever"
    repMaxReps.isNotEmpty() -> "New ${repMaxReps.min()}-rep max"
    RecordKind.SetVolume in kinds -> "Best set volume"
    RecordKind.SessionVolume in kinds -> "Best session volume"
    else -> "New personal record"
}
