package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.LiveWorkoutViewModel
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.InlineHint
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.MusicMiniControls
import com.lukr99.workout.ui.components.PrBanner
import com.lukr99.workout.ui.components.RestTimerBar
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.components.WorkoutNoteRow
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * The live logging loop — start/resume → add exercises → log sets → rest → finish/discard. The
 * highest-priority Phase 2 screen. All persistence goes through [LiveWorkoutViewModel] → repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWorkoutScreen(
    vm: LiveWorkoutViewModel,
    units: UnitSystem,
    onClose: () -> Unit,
    onEditExercise: (String) -> Unit,
    onOpenHistory: (String) -> Unit = {},
    onWorkoutSaved: suspend (WorkoutSession) -> Unit = {},
) {
    val toast = LocalToast.current
    val draft by vm.draft.collectAsState()
    val doneIds by vm.doneSetIds.collectAsState()
    val rest by vm.rest.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val prEvent by vm.prEvent.collectAsState()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    val suggestion by vm.suggestion.collectAsState()
    LaunchedEffect(Unit) { vm.loadActiveIfAny() }
    // Flush the working draft when the app is backgrounded, so unsaved reps/weight survive if the OS
    // reclaims the process while a live session is open.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) vm.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(suggestion) {
        suggestion?.let { toast(it); vm.consumeSuggestion() }
    }
    LaunchedEffect(prEvent?.id) {
        if (prEvent != null) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(4000)
            vm.consumePrEvent()
        }
    }
    // Haptic when a running rest hits zero (skip/reset leaves total at 0, so it stays silent).
    LaunchedEffect(rest.remaining, rest.total) {
        if (rest.total > 0 && rest.remaining == 0) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    var sheet by remember { mutableStateOf<LiveSheet?>(null) }
    var expandedFinishedEntries by remember { mutableStateOf(emptySet<String>()) }
    // Exercises further down start folded to one row; opening one keeps it open for this visit.
    var openedEntries by remember { mutableStateOf(emptySet<String>()) }
    val catalog by vm.catalogById.collectAsState()
    val previousNotes by vm.previousNotes.collectAsState()
    val previousSets by vm.previousSets.collectAsState()
    val recentIds by produceState(emptyList<String>()) { value = vm.history.recentExerciseIds() }
    val repeatable by produceState(emptyList<WorkoutSession>()) { value = vm.history.repeatable() }
    val templates by produceState(emptyList<WorkoutTemplate>()) { value = vm.history.templates() }
    val currentSetId = nextSet(draft?.entries.orEmpty(), doneIds)?.second?.id
    var nowUtcMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            nowUtcMillis = System.currentTimeMillis()
        }
    }

    val session = draft
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LiveTopBar(
                name = session?.name ?: "Workout",
                summary = listOf(
                    Format.duration(((nowUtcMillis - (session?.startedAtUtc ?: nowUtcMillis)) / 1000).coerceAtLeast(0)),
                    "${Format.volume(vm.estimatedVolumeKg(), units)} ${Format.unitLabel(units)}",
                    "${doneIds.size} ${if (doneIds.size == 1) "set" else "sets"}",
                ).joinToString(" \u00b7 "),
                onRename = vm::rename,
                onMinimise = onClose,
                onFinish = { sheet = LiveSheet.ConfirmFinish },
            )

            val entries = session?.entries.orEmpty()
            val supersetMembers = entries.filter { it.supersetGroup != null }
                .groupBy { checkNotNull(it.supersetGroup) }
            val compactIds = compactEntryIds(entries, doneIds) - openedEntries
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 14.dp, end = 14.dp, top = 4.dp, bottom = 160.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "workout-note") {
                    WorkoutNoteRow(
                        note = session?.notes.orEmpty(),
                        onEdit = { sheet = LiveSheet.Note(NoteTarget.Workout) },
                        modifier = Modifier.testTag(LiveWorkoutTags.WORKOUT_NOTE),
                    )
                }
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    if (entry.id in compactIds) {
                        CompactEntryRow(entry, onOpen = { openedEntries = openedEntries + entry.id })
                        return@itemsIndexed
                    }
                    val catalogExercise = catalog[entry.exerciseId]
                    LiveEntryCard(
                        entry = entry,
                        units = units,
                        doneIds = doneIds,
                        currentSetId = currentSetId,
                        supersetPosition = entry.supersetGroup?.let { group ->
                            supersetMembers[group].orEmpty().indexOfFirst { it.id == entry.id } + 1
                        },
                        supersetSize = entry.supersetGroup?.let { supersetMembers[it].orEmpty().size } ?: 0,
                        collapsed = entry.completedAtUtc != null && entry.id !in expandedFinishedEntries,
                        nowUtcMillis = nowUtcMillis,
                        notes = EntryCardNotes(
                            exerciseNote = catalogExercise?.notes.orEmpty(),
                            previous = previousNotes[entry.exerciseId],
                            hasGuide = catalogExercise != null,
                            lastTimeSets = previousSets[entry.exerciseId].orEmpty(),
                        ),
                        actions = EntryCardActions(
                            onOpenMenu = { sheet = LiveSheet.Menu(entry.id) },
                            onEditSuperset = { entry.supersetGroup?.let { sheet = LiveSheet.Superset(it) } },
                            onToggleCollapsed = {
                                expandedFinishedEntries = if (entry.id in expandedFinishedEntries) {
                                    expandedFinishedEntries - entry.id
                                } else {
                                    expandedFinishedEntries + entry.id
                                }
                            },
                            onToggleWeightUnit = { vm.toggleEntryWeightUnit(entry.id, units) },
                            onStart = { vm.startEntry(entry.id) },
                            onFinish = {
                                expandedFinishedEntries = expandedFinishedEntries - entry.id
                                vm.finishEntry(entry.id)
                            },
                            onReopen = { vm.reopenEntry(entry.id) },
                            onReps = { setId, reps -> vm.setReps(entry.id, setId, reps) },
                            onWeight = { setId, kg -> vm.setWeight(entry.id, setId, kg) },
                            onToggleDone = { setId ->
                                vm.toggleSetDone(entry.id, setId)
                                if (setId !in doneIds) toast("Set logged")
                            },
                            onOptions = { setId -> sheet = LiveSheet.SetOptions(entry.id, setId) },
                            onAddSet = { vm.addSet(entry.id) },
                            onCardioChange = { data -> vm.updateCardio(entry.id) { data } },
                            onEditNote = { sheet = LiveSheet.Note(NoteTarget.Entry(entry.id)) },
                            onShowGuide = { catalogExercise?.let { sheet = LiveSheet.Guide(it) } },
                        ),
                    )
                }
                if (entries.isEmpty()) {
                    item(key = "empty-start") {
                        EmptyWorkoutStart(
                            recent = recentIds.mapNotNull(catalog::get).filterNot { it.isArchived },
                            onAddExercises = { sheet = LiveSheet.AddExercise },
                            onQuickAdd = { vm.addExercise(it); toast("${it.name} added") },
                            repeatable = repeatable,
                            templates = templates,
                            onRepeat = { vm.repeatWorkout(it); toast("Copied ${it.name}") },
                            onSwitch = { vm.switchToTemplate(it); toast("Switched to ${it.name}") },
                        )
                    }
                } else {
                    item(key = "set-hint") {
                        InlineHint(Icons.Rounded.TouchApp, "Tap a set's number for tags, effort, a note, or to remove it.")
                    }
                    item {
                        AddButton("Add exercises", Modifier.testTag(LiveWorkoutTags.ADD_EXERCISE)) { sheet = LiveSheet.AddExercise }
                    }
                }
            }
        }

        // PR celebration overlay (top)
        androidx.compose.animation.AnimatedVisibility(
            visible = prEvent != null,
            // Sit below the top bar (title/Finish) so the celebration isn't occluded by — or fighting
            // the app-level toast for — the very top of the screen.
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 60.dp),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it },
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
        ) {
            prEvent?.let { ev ->
                androidx.compose.runtime.key(ev.id) {
                    PrBanner(
                        exerciseName = ev.exerciseName,
                        headline = ev.headline,
                        displayValue = Format.toDisplay(ev.estimated1RmKg, units),
                        unitLabel = Format.unitLabel(units),
                    )
                }
            }
        }

        // Sticky bottom: rest timer + discard
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (rest.running) {
                RestTimerBar(
                    remainingSeconds = rest.remaining,
                    totalSeconds = rest.total,
                    onAdd15 = { vm.addRest(15) },
                    onSkip = { vm.skipRest() },
                    next = nextSetLabel(draft?.entries.orEmpty(), doneIds),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = { sheet = LiveSheet.ConfirmDiscard },
                    modifier = Modifier.weight(1f).testTag(LiveWorkoutTags.DISCARD)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    LiveWorkoutSheets(
        sheet = sheet,
        session = session,
        vm = vm,
        units = units,
        exercises = exercises,
        catalog = catalog,
        onSheet = { sheet = it },
        onEditExercise = onEditExercise,
        onOpenHistory = onOpenHistory,
        onWorkoutSaved = onWorkoutSaved,
        onClose = onClose,
        toast = { toast(it) },
    )
}

@Composable
private fun AddButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text("  $label", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LiveTopBar(
    name: String,
    summary: String,
    onRename: (String) -> Unit,
    onMinimise: () -> Unit,
    onFinish: () -> Unit,
) {
    val colors = EmberTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundIconButton(Icons.Rounded.KeyboardArrowDown, "Minimise workout", onMinimise)
        Column(Modifier.weight(1f)) {
            BasicTextField(
                value = name,
                onValueChange = onRename,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
            )
            Text(summary, style = Numbers.copy(fontSize = 14.sp), color = colors.textSecondary, maxLines = 1)
        }
        MusicMiniControls()
        Box(
            Modifier.testTag(LiveWorkoutTags.FINISH).height(44.dp).clip(RoundedCornerShape(50)).background(colors.primary)
                .clickable(role = Role.Button, onClick = onFinish).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Finish", fontWeight = FontWeight.Bold, color = colors.onPrimary)
        }
    }
}
