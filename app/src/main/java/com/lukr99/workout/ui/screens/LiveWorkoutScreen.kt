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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.LiveWorkoutViewModel
import com.lukr99.workout.ui.stats
import com.lukr99.workout.ui.statsSummary
import com.lukr99.workout.ui.components.ConfirmDialog
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.PrBanner
import com.lukr99.workout.ui.components.RestTimerBar
import com.lukr99.workout.ui.components.MusicMiniControls
import com.lukr99.workout.ui.components.SetColumnHeader
import com.lukr99.workout.ui.components.SetRow
import com.lukr99.workout.ui.components.Tag
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
    onCreateExercise: (String) -> Unit,
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

    var showPicker by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<Pair<String, String>?>(null) }
    var expandedFinishedEntries by remember { mutableStateOf(emptySet<String>()) }
    var editingSuperset by remember { mutableStateOf<Int?>(null) }
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
                MusicMiniControls()
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = { confirmFinish = true }) {
                    Text("Finish", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            val entries = session?.entries.orEmpty()
            val supersetMembers = entries.filter { it.supersetGroup != null }
                .groupBy { checkNotNull(it.supersetGroup) }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 14.dp, end = 14.dp, top = 4.dp, bottom = 160.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    EntryCard(
                        entry = entry,
                        units = units,
                        doneIds = doneIds,
                        canGroupWithPrevious = index > 0,
                        groupedWithPrevious = index > 0 &&
                            entry.supersetGroup != null &&
                            entry.supersetGroup == entries[index - 1].supersetGroup,
                        supersetPosition = entry.supersetGroup?.let { group ->
                            supersetMembers[group].orEmpty().indexOfFirst { it.id == entry.id } + 1
                        },
                        supersetSize = entry.supersetGroup?.let { supersetMembers[it].orEmpty().size } ?: 0,
                        collapsed = entry.completedAtUtc != null && entry.id !in expandedFinishedEntries,
                        nowUtcMillis = nowUtcMillis,
                        onToggleSuperset = { vm.toggleSupersetWithPrevious(entry.id) },
                        onEditSuperset = { entry.supersetGroup?.let { editingSuperset = it } },
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
                        onOptions = { setId -> optionsFor = entry.id to setId },
                        onAddSet = { vm.addSet(entry.id) },
                        onMoveUp = { vm.moveEntry(entry.id, up = true) },
                        onMoveDown = { vm.moveEntry(entry.id, up = false) },
                        onRemove = { vm.removeEntry(entry.id) },
                        onCardioChange = { data -> vm.updateCardio(entry.id) { data } },
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
                onCreate = {
                    showPicker = false
                    onCreateExercise(it)
                },
            )
        }
    }

    optionsFor?.let { (entryId, setId) ->
        val entry = session?.entries?.firstOrNull { it.id == entryId }
        val set = entry?.strengthSets?.firstOrNull { it.id == setId }
        if (set != null) {
            com.lukr99.workout.ui.components.SetOptionsSheet(
                set = set,
                onToggleTag = { vm.toggleSetTag(entryId, setId, it) },
                onRir = { vm.setRir(entryId, setId, it) },
                onRpe = { vm.setRpe(entryId, setId, it) },
                onRemove = { vm.removeSet(entryId, setId) },
                onDismiss = { optionsFor = null },
            )
        } else optionsFor = null
    }

    editingSuperset?.let { groupId ->
        SupersetEditorSheet(
            groupId = groupId,
            entries = session?.entries.orEmpty(),
            onAddBefore = { vm.extendSuperset(groupId, before = true); editingSuperset = null },
            onAddAfter = { vm.extendSuperset(groupId, before = false); editingSuperset = null },
            onRemoveEntry = { vm.removeFromSuperset(it); editingSuperset = null },
            onUngroup = { vm.ungroupSuperset(groupId); editingSuperset = null },
            onDismiss = { editingSuperset = null },
        )
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
    canGroupWithPrevious: Boolean,
    groupedWithPrevious: Boolean,
    supersetPosition: Int?,
    supersetSize: Int,
    collapsed: Boolean,
    nowUtcMillis: Long,
    onToggleSuperset: () -> Unit,
    onEditSuperset: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onToggleWeightUnit: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onReopen: () -> Unit,
    onReps: (String, Int) -> Unit,
    onWeight: (String, Double) -> Unit,
    onToggleDone: (String) -> Unit,
    onOptions: (String) -> Unit,
    onAddSet: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onCardioChange: (com.lukr99.workout.domain.CardioEntryData) -> Unit,
) {
    val entryUnits = Format.entryUnits(entry.weightUnitOverride, units)
    val stats = entry.stats(nowUtcMillis)
    // The superset rail is painted behind the row instead of being a `fillMaxHeight` sibling under
    // `Modifier.height(IntrinsicSize.Min)`. Intrinsic measurement walks the whole card, and any
    // scrolling or lazy content inside a set row cannot answer it — a set earning a PR chip used to
    // crash the app outright, then again on every relaunch because `isPr` is persisted.
    val railColor = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().then(
            if (entry.supersetGroup == null) {
                Modifier
            } else {
                Modifier.drawBehind {
                    val railWidth = 3.dp.toPx()
                    drawRoundRect(
                        color = railColor,
                        size = Size(railWidth, size.height),
                        cornerRadius = CornerRadius(railWidth / 2f),
                    )
                }
            },
        ),
    ) {
        if (entry.supersetGroup != null) Spacer(Modifier.width(10.dp))
        Column(
            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (entry.supersetGroup != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SUPERSET · ${supersetPosition ?: 1} OF $supersetSize",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (supersetPosition == 1) {
                        TextButton(onClick = onEditSuperset) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp))
                            Text(" Edit")
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.exerciseSnapshotName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (entry.exerciseSnapshotPrimaryBodyPart.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Tag(
                            entry.exerciseSnapshotPrimaryBodyPart,
                            accent = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Text(
                        when {
                            entry.completedAtUtc != null -> "Finished · ${Format.duration(stats.durationSeconds ?: 0)}"
                            entry.startedAtUtc != null -> "In progress · ${Format.duration(stats.durationSeconds ?: 0)}"
                            else -> "Not started"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.completedAtUtc != null) MaterialTheme.colorScheme.primary else TextMid,
                    )
                }
                if (entry.isStrength) {
                    TextButton(onClick = onToggleWeightUnit) {
                        Text(Format.unitLabel(entryUnits).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
                if (entry.completedAtUtc != null) {
                    IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            if (collapsed) "Expand finished exercise" else "Collapse finished exercise",
                            tint = TextMid,
                        )
                    }
                }
                if (canGroupWithPrevious) {
                    IconButton(onClick = onToggleSuperset, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Rounded.Link,
                            if (groupedWithPrevious) "Ungroup from previous" else "Superset with previous",
                            tint = if (groupedWithPrevious) MaterialTheme.colorScheme.primary else TextMid,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                IconButton(onClick = onMoveUp, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.ArrowUpward,
                        "Move up",
                        tint = TextMid,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        "Move down",
                        tint = TextMid,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        "Remove exercise",
                        tint = TextMid,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (collapsed) {
                Text(
                    entry.statsSummary(entryUnits, nowUtcMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                stats.bestSet?.let { best ->
                    Text(
                        "Best set ${best.reps} × ${Format.weightWithUnit(best.weightKg, entryUnits)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMid,
                    )
                }
            } else if (entry.isStrength) {
                if (entry.strengthSets.isNotEmpty()) {
                    SetColumnHeader(entryUnits)
                }
                entry.strengthSets.forEachIndexed { index, set ->
                    SetRow(
                        index = index,
                        set = set,
                        units = entryUnits,
                        done = set.id in doneIds,
                        onReps = { onReps(set.id, it) },
                        onWeightKg = { onWeight(set.id, it) },
                        onToggleDone = { onToggleDone(set.id) },
                        onOptions = { onOptions(set.id) },
                    )
                }
                TextButton(onClick = onAddSet) {
                    Icon(
                        Icons.Rounded.Add,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" Add set", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                com.lukr99.workout.ui.components.CardioEditor(
                    cardio = entry.cardioData
                        ?: com.lukr99.workout.domain.CardioEntryData(workoutEntryId = entry.id),
                    onChange = onCardioChange,
                )
            }
            if (!collapsed) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when {
                        entry.completedAtUtc != null -> TextButton(onClick = onReopen) {
                            Text("Reopen exercise")
                        }
                        entry.startedAtUtc == null -> TextButton(onClick = onStart) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Text(" Start exercise")
                        }
                        else -> TextButton(onClick = onFinish) {
                            Text("Finish exercise", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupersetEditorSheet(
    groupId: Int,
    entries: List<WorkoutEntry>,
    onAddBefore: () -> Unit,
    onAddAfter: () -> Unit,
    onRemoveEntry: (String) -> Unit,
    onUngroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val positions = entries.indices.filter { entries[it].supersetGroup == groupId }
    val members = positions.map(entries::get)
    val canAddBefore = positions.firstOrNull()?.let { it > 0 && entries[it - 1].supersetGroup == null } == true
    val canAddAfter = positions.lastOrNull()?.let { it < entries.lastIndex && entries[it + 1].supersetGroup == null } == true
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Edit superset", style = MaterialTheme.typography.titleLarge)
            Text("Exercises stay together and are performed as one round.", color = TextMid)
            members.forEachIndexed { index, entry ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}. ${entry.exerciseSnapshotName}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { onRemoveEntry(entry.id) }) { Text("Remove") }
                }
            }
            if (canAddBefore) TextButton(onClick = onAddBefore) {
                Text("+ Add ${entries[positions.first() - 1].exerciseSnapshotName} before")
            }
            if (canAddAfter) TextButton(onClick = onAddAfter) {
                Text("+ Add ${entries[positions.last() + 1].exerciseSnapshotName} after")
            }
            TextButton(onClick = onUngroup) { Text("Ungroup superset", color = MaterialTheme.colorScheme.error) }
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

