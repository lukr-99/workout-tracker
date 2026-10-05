package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.replacedWith
import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.completedAt
import com.lukr99.workout.domain.setIdsMarkedDone
import com.lukr99.workout.domain.withEntriesAdded
import com.lukr99.workout.domain.withEntry
import com.lukr99.workout.domain.withEntryFinished
import com.lukr99.workout.domain.withEntryMoved
import com.lukr99.workout.domain.withEntryStarted
import com.lukr99.workout.domain.withSet
import com.lukr99.workout.domain.withSetAdded
import com.lukr99.workout.domain.withSetDone
import com.lukr99.workout.domain.withTagToggled
import com.lukr99.workout.domain.withWeightUnitToggled
import com.lukr99.workout.domain.effectiveTags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val restTimer = RestTimer(viewModelScope)
    val rest: StateFlow<RestState> = restTimer.state

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

    /** Every catalog row by id, archived included, so a logged exercise always finds its notes. */
    val catalogById: StateFlow<Map<String, Exercise>> =
        repo.observeExercises(ExerciseFilter(includeArchived = true))
            .map { rows -> rows.associateBy(Exercise::id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Read-only look-ups into earlier workouts, for the sheets (history, recents, last time). */
    val history = EntryHistory(repo, insights)
    val templates = TemplateSync(repo)

    /** The live workout's id and exercise ids; "last time" data reloads only when these change. */
    private val onScreen = draftState
        .map { session -> session?.let { it.id to it.entries.map(WorkoutEntry::exerciseId).toSet() } }
        .distinctUntilChanged()

    /** "Last time" notes for the exercises in the live session. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val previousNotes: StateFlow<Map<String, PreviousEntryNote>> =
        onScreen.mapLatest { key -> key?.let { (sessionId, ids) -> history.notes(ids, sessionId) }.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Last time's sets for the exercises in the live session, for each set row's "Previous". */
    @OptIn(ExperimentalCoroutinesApi::class)
    val previousSets: StateFlow<Map<String, List<StrengthSet>>> =
        onScreen.mapLatest { key -> key?.let { (_, ids) -> history.lastSets(ids) }.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val persistMutex = Mutex()
    private var persistJob: Job? = null
    private var finalizing = false

    /** Ensure a live session exists (resumes an existing one) and load it into the draft. */
    fun startOrResume(templateId: String? = null) {
        viewModelScope.launch {
            val session = repo.createWorkoutSession(templateId = templateId)
            draftState.value = session
            doneSetIdsState.value = session.setIdsMarkedDone
        }
    }

    fun loadActiveIfAny() {
        if (draftState.value != null) return
        viewModelScope.launch {
            repo.getActiveSession()?.let {
                draftState.value = it
                doneSetIdsState.value = it.setIdsMarkedDone
            }
        }
    }

    // A resumed workout (after backgrounding or process death) shows the same checkmarks, because
    // completion is stored on each set as performedAtUtc.

    /** Flush the working draft to the repository — called when the live screen is left/backgrounded. */
    fun flush() = persist()

    // --- Structural edits ----------------------------------------------------------------------

    fun rename(name: String) = mutate(persist = false) { it.copy(name = name) }

    // --- Notes (saved right away; each comes from an explicit Save in the note sheet) ----------

    fun setWorkoutNote(text: String) = mutate { it.copy(notes = text.trim()) }

    fun setEntryNote(entryId: String, text: String) =
        mutate { it.withEntry(entryId) { entry -> entry.copy(notes = text.trim()) } }

    fun setSetNote(entryId: String, setId: String, text: String) =
        mutate { it.withSet(entryId, setId) { set -> set.copy(notes = text.trim()) } }

    /** Adds [exercise] at the end, starting from the suggested or last time's sets when there are any. */
    fun addExercise(exercise: Exercise) = addExercises(listOf(exercise), asSuperset = false)

    /** Adds several exercises at the end in order, grouped as one superset when asked and when there are two or more. */
    fun addExercises(exercises: List<Exercise>, asSuperset: Boolean) {
        if (exercises.isEmpty()) return
        viewModelScope.launch {
            val start = draftState.value?.entries?.size ?: 0
            val added = exercises.mapIndexed { i, exercise ->
                val base = repo.newEntryForExercise(exercise, sortOrder = start + i)
                val prefill = history.prefill(exercise)
                prefill?.rationale?.let { suggestionState.value = it }
                prefill?.let { base.copy(strengthSets = it.sets) } ?: base
            }
            mutate { it.withEntriesAdded(added, asSuperset) }
        }
    }

    /** Saves a new exercise from the quick form to the library and adds it to the workout. */
    fun createAndAdd(exercise: Exercise) {
        viewModelScope.launch { addExercise(repo.saveExercise(exercise)) }
    }

    /** Swaps an exercise for another (the machine is taken) and keeps its logged sets. */
    fun replaceExercise(entryId: String, exercise: Exercise) =
        mutate { it.withEntry(entryId) { entry -> entry.replacedWith(exercise) } }

    fun removeEntry(entryId: String) = mutate { session ->
        session.copy(entries = normalizeSupersetGroups(session.entries.filterNot { it.id == entryId }))
    }

    fun moveEntry(entryId: String, up: Boolean) = mutate { session ->
        val moved = session.withEntryMoved(entryId, up)
        moved.copy(entries = normalizeSupersetGroups(moved.entries))
    }

    /** Join/ungroup this exercise with the previous consecutive entry. */
    fun toggleSupersetWithPrevious(entryId: String) = mutate { session ->
        session.copy(entries = toggleSupersetBoundary(session.entries, entryId))
    }

    fun ungroupSuperset(groupId: Int) = mutate { session ->
        session.copy(entries = clearSupersetGroup(session.entries, groupId))
    }

    fun removeFromSuperset(entryId: String) = mutate { session ->
        session.copy(entries = removeFromSupersetGroup(session.entries, entryId))
    }

    fun extendSuperset(groupId: Int, before: Boolean) = mutate { session ->
        session.copy(entries = extendSupersetGroup(session.entries, groupId, before))
    }

    fun toggleEntryWeightUnit(entryId: String, fallback: com.lukr99.workout.settings.UnitSystem) = mutate {
        it.withEntry(entryId) { entry ->
            entry.withWeightUnitToggled(poundsByDefault = fallback == com.lukr99.workout.settings.UnitSystem.Imperial)
        }
    }

    fun startEntry(entryId: String) = mutate { it.withEntryStarted(entryId, System.currentTimeMillis()) }

    fun finishEntry(entryId: String) = mutate { it.withEntryFinished(entryId, System.currentTimeMillis()) }

    fun reopenEntry(entryId: String) = mutate { it.withEntry(entryId) { entry -> entry.copy(completedAtUtc = null) } }

    fun addSet(entryId: String) = mutate { it.withEntry(entryId, WorkoutEntry::withSetAdded) }

    fun removeSet(entryId: String, setId: String) = mutate {
        it.withEntry(entryId) { entry -> entry.copy(strengthSets = entry.strengthSets.filterNot { set -> set.id == setId }) }
    }

    // --- Set field edits (in-memory only; flushed on done/finish) -------------------------------

    fun updateSet(entryId: String, setId: String, transform: (StrengthSet) -> StrengthSet) =
        mutate(persist = false) { it.withSet(entryId, setId, transform) }

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
        mutate(persist = false) {
            it.withEntry(entryId) { entry ->
                entry.copy(cardioData = transform(entry.cardioData ?: com.lukr99.workout.domain.CardioEntryData(workoutEntryId = entry.id)))
            }
        }

    /** Toggle one tag without disturbing the others; the legacy single type stays export-friendly. */
    fun toggleSetTag(entryId: String, setId: String, tag: SetTag) =
        mutate { it.withSet(entryId, setId) { set -> set.withTagToggled(tag) } }

    /** Mark/unmark a set done. Both directions persist the draft; marking done starts the rest timer. */
    fun toggleSetDone(entryId: String, setId: String) {
        val currentlyDone = setId in doneSetIdsState.value
        doneSetIdsState.update { if (currentlyDone) it - setId else it + setId }
        // Stamp completion onto the set itself so the checkmark (and the typed reps/weight) survive
        // leaving the screen or the OS reclaiming the process — doneSetIds alone is in-memory only.
        val stamp = if (currentlyDone) null else System.currentTimeMillis()
        mutate(persist = false) { it.withSetDone(entryId, setId, stamp) }
        if (!currentlyDone) {
            val entry = draftState.value?.entries?.firstOrNull { it.id == entryId }
            val restSecs = entry?.let { restSecondsFor(it) } ?: defaultRest
            startRest(restSecs)
            evaluatePr(entryId, setId)
        }
        persist()
    }

    /**
     * On set-done, ask Phase 3.5 `insights.evaluateSetRecord` whether the entered set beats a stored
     * record. If so, flag the set as a PR (persisted, so it shows the PR badge everywhere) and emit a
     * [PrEvent] the screen turns into the count-up + glow + haptic.
     */
    private fun evaluatePr(entryId: String, setId: String) {
        val entry = draftState.value?.entries?.firstOrNull { it.id == entryId } ?: return
        val set = entry.strengthSets.firstOrNull { it.id == setId } ?: return
        if (entry.exerciseId.isBlank() || set.isWarmup || SetTag.Warmup in set.effectiveTags) return
        if (set.reps <= 0 || set.weightKg <= 0.0) return
        viewModelScope.launch {
            val achievement = insights.evaluateSetRecord(entry.exerciseId, set)
            if (!achievement.isPersonalRecord) return@launch
            // Flag the set as a PR in the draft (persist so history keeps the badge).
            mutate { it.withSet(entryId, setId) { s -> s.copy(isPr = true) } }
            prEventState.value = PrEvent(
                id = System.nanoTime(),
                exerciseName = entry.exerciseSnapshotName,
                estimated1RmKg = Estimates.epley(set.weightKg, set.reps),
                headline = achievement.headline,
            )
        }
    }

    // --- Rest timer ----------------------------------------------------------------------------

    fun startRest(seconds: Int) = restTimer.start(seconds)

    fun addRest(seconds: Int) = restTimer.add(seconds)

    fun skipRest() = restTimer.skip()

    // --- Finish / discard ----------------------------------------------------------------------

    /** Saves the workout as finished, then runs [afterSave] with it (the template update, if any). */
    fun finish(afterSave: suspend (WorkoutSession) -> Unit = {}, onDone: () -> Unit) {
        val session = draftState.value ?: return onDone()
        if (finalizing) return
        finalizing = true
        viewModelScope.launch {
            try {
                persistJob?.cancelAndJoin()
                val saved = persistMutex.withLock { repo.saveWorkoutSession(session.completedAt(System.currentTimeMillis())) }
                runCatching { afterSave(saved) }
                clear()
                onDone()
            } finally {
                finalizing = false
            }
        }
    }

    fun discard(onDone: () -> Unit) {
        val session = draftState.value ?: return onDone()
        if (finalizing) return
        finalizing = true
        viewModelScope.launch {
            try {
                persistJob?.cancelAndJoin()
                persistMutex.withLock {
                    repo.saveWorkoutSession(
                        session.copy(
                            status = WorkoutSessionStatus.Discarded,
                            endedAtUtc = System.currentTimeMillis(),
                        ),
                    )
                }
                clear()
                onDone()
            } finally {
                finalizing = false
            }
        }
    }

    // --- Helpers -------------------------------------------------------------------------------

    fun estimatedVolumeKg(): Double =
        draftState.value?.entries?.sumOf { Estimates.volume(it.strengthSets) } ?: 0.0

    private fun restSecondsFor(entry: WorkoutEntry): Int {
        val fromCatalog = exercises.value.firstOrNull { it.id == entry.exerciseId }?.defaultRestSeconds
        return fromCatalog ?: defaultRest
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
     * Cancel-and-replace flush of the working draft. Only one repository write can run at a time,
     * and a newer snapshot supersedes a stale save that is queued or in flight. Ids are assigned
     * client-side and the repository preserves non-blank ids, so we deliberately do NOT read the
     * result back; that would clobber reps/weight typed while the save was in flight.
     */
    private fun persist() {
        if (finalizing) return
        val session = draftState.value ?: return
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            persistMutex.withLock { repo.saveWorkoutSession(session) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LiveWorkoutViewModel(container.repository, container.settings, container.insights) as T
        }
    }
}
