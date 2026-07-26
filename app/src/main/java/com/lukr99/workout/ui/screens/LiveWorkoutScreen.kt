package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.LiveWorkoutViewModel
import com.lukr99.workout.ui.components.ChoiceDialog
import com.lukr99.workout.ui.components.ConfirmDialog
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.PrBanner
import com.lukr99.workout.ui.components.RestTimerBar
import com.lukr99.workout.ui.components.SetColumnHeader
import com.lukr99.workout.ui.components.SetRow
import com.lukr99.workout.ui.components.Tag
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

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
    LaunchedEffect(suggestion) {
        suggestion?.let { toast(it); vm.consumeSuggestion() }
    }
    LaunchedEffect(prEvent?.id) {
        if (prEvent != null) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(2800)
            vm.consumePrEvent()
        }
    }

    var showPicker by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<Pair<String, String>?>(null) }

    val session = draft
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, "Minimise", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    BasicTextField(
                        value = session?.name ?: "Workout",
                        onValueChange = { vm.rename(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.merge(
                            androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onBackground),
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        "Volume ${Format.volume(vm.estimatedVolumeKg(), units)} ${Format.unitLabel(units)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMid,
                    )
                }
                TextButton(onClick = { confirmFinish = true }) {
                    Text("Finish", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            val entries = session?.entries.orEmpty()
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 14.dp, end = 14.dp, top = 4.dp, bottom = 160.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        units = units,
                        doneIds = doneIds,
                        onReps = { setId, reps -> vm.setReps(entry.id, setId, reps) },
                        onWeight = { setId, kg -> vm.setWeight(entry.id, setId, kg) },
                        onToggleDone = { setId ->
                            vm.toggleSetDone(entry.id, setId)
                            if (setId !in doneIds) toast("Set logged")
                        },
                        onOptions = { setId -> optionsFor = entry.id to setId },
                        onAddSet = { vm.addSet(entry.id) },
                        onMoveUp = { vm.moveEntry(entry.id, up = true) },
                        onMoveDown = { vm.moveEntry(entry.id, up = false) },
                        onRemove = { vm.removeEntry(entry.id) },
                    )
                }
                item {
                    AddButton("Add exercise") { showPicker = true }
                }
                if (entries.isEmpty()) {
                    item {
                        Text(
                            "No exercises yet — add one to start logging.",
                            style = MaterialTheme.typography.bodyLarge, color = TextMid,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        // PR celebration overlay (top)
        androidx.compose.animation.AnimatedVisibility(
            visible = prEvent != null,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
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
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = { confirmDiscard = true },
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ExercisePicker(
                exercises = exercises,
                onPick = {
                    vm.addExercise(it)
                    showPicker = false
                    toast("${it.name} added")
                },
            )
        }
    }

    optionsFor?.let { (entryId, setId) ->
        val entry = session?.entries?.firstOrNull { it.id == entryId }
        val set = entry?.strengthSets?.firstOrNull { it.id == setId }
        if (set != null) {
            ChoiceDialog(
                title = "Set type",
                options = SetType.entries,
                selected = set.setType,
                label = { it.name },
                onSelect = { vm.setType(entryId, setId, it) },
                onDismiss = { optionsFor = null },
            )
        } else optionsFor = null
    }

    if (confirmFinish) {
        ConfirmDialog(
            title = "Finish workout?",
            message = "Empty exercises are dropped. This saves the session to your history.",
            confirmLabel = "Finish",
            onConfirm = { vm.finish { onClose(); toast("Workout saved") } },
            onDismiss = { confirmFinish = false },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard workout?",
            message = "This session will not be saved to your history.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { vm.discard { onClose() } },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun EntryCard(
    entry: WorkoutEntry,
    units: UnitSystem,
    doneIds: Set<String>,
    onReps: (String, Int) -> Unit,
    onWeight: (String, Double) -> Unit,
    onToggleDone: (String) -> Unit,
    onOptions: (String) -> Unit,
    onAddSet: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.exerciseSnapshotName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (entry.exerciseSnapshotPrimaryBodyPart.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Tag(entry.exerciseSnapshotPrimaryBodyPart, accent = MaterialTheme.colorScheme.secondary)
                }
            }
            IconButton(onClick = onMoveUp, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.ArrowUpward, "Move up", tint = TextMid, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.ArrowDownward, "Move down", tint = TextMid, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Delete, "Remove exercise", tint = TextMid, modifier = Modifier.size(18.dp))
            }
        }

        if (entry.isStrength) {
            if (entry.strengthSets.isNotEmpty()) {
                SetColumnHeader(units)
            }
            entry.strengthSets.forEachIndexed { index, set ->
                SetRow(
                    index = index,
                    set = set,
                    units = units,
                    done = set.id in doneIds,
                    onReps = { onReps(set.id, it) },
                    onWeightKg = { onWeight(set.id, it) },
                    onToggleDone = { onToggleDone(set.id) },
                    onOptions = { onOptions(set.id) },
                )
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(" Add set", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Text("Cardio entry", style = MaterialTheme.typography.bodyLarge, color = TextMid)
        }
    }
}

@Composable
private fun AddButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
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

