package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.newId
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HistoryViewModel
import com.lukr99.workout.ui.components.ConfirmDialog
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.SetColumnHeader
import com.lukr99.workout.ui.components.Tag
import com.lukr99.workout.ui.components.ValueCell
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/** Drill-in for a past session with full edit-after-the-fact (persists via saveWorkoutSession). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    vm: HistoryViewModel,
    sessionId: String,
    units: UnitSystem,
    onBack: () -> Unit,
) {
    LaunchedEffect(sessionId) { vm.open(sessionId) }
    val selected by vm.selected.collectAsState()
    val exercises by vm.exercises.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    var draft by remember(sessionId) { mutableStateOf<WorkoutSession?>(null) }
    var seeded by remember(sessionId) { mutableStateOf(false) }
    if (selected != null && selected?.id == sessionId && !seeded) {
        draft = selected
        seeded = true
    }
    var confirmDelete by remember { mutableStateOf(false) }

    val session = draft
    if (session == null) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Loading…", color = TextMid)
        }
        return
    }

    fun mutateEntry(entryId: String, transform: (WorkoutEntry) -> WorkoutEntry) {
        draft = session.copy(entries = session.entries.map { if (it.id == entryId) transform(it) else it })
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                BasicTextField(
                    value = session.name,
                    onValueChange = { draft = session.copy(name = it) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onBackground)),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                )
                Text(Format.fullDate(session.completedDateUtc ?: session.startedAtUtc), style = MaterialTheme.typography.labelSmall, color = TextMid)
            }
            TextButton(onClick = { vm.save(session) { onBack() } }) {
                Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(session.entries, key = { it.id }) { entry ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.exerciseSnapshotName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            if (entry.exerciseSnapshotPrimaryBodyPart.isNotBlank()) {
                                Tag(entry.exerciseSnapshotPrimaryBodyPart, accent = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        IconButton(onClick = { draft = session.copy(entries = session.entries.filterNot { it.id == entry.id }) }) {
                            Icon(Icons.Rounded.Delete, "Remove exercise", tint = TextMid, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (entry.isStrength) {
                        if (entry.strengthSets.isNotEmpty()) {
                            SetColumnHeader(units)
                        }
                        entry.strengthSets.forEachIndexed { i, set ->
                            EditSetRow(
                                index = i, set = set, units = units,
                                onReps = { reps -> mutateEntry(entry.id) { e -> e.copy(strengthSets = e.strengthSets.map { if (it.id == set.id) it.copy(reps = reps) else it }) } },
                                onWeight = { kg -> mutateEntry(entry.id) { e -> e.copy(strengthSets = e.strengthSets.map { if (it.id == set.id) it.copy(weightKg = kg) else it }) } },
                                onRemove = { mutateEntry(entry.id) { e -> e.copy(strengthSets = e.strengthSets.filterNot { it.id == set.id }) } },
                            )
                        }
                        TextButton(onClick = {
                            mutateEntry(entry.id) { e ->
                                val last = e.strengthSets.lastOrNull()
                                e.copy(strengthSets = e.strengthSets + StrengthSet(id = newId(), workoutEntryId = e.id, setNumber = e.strengthSets.size + 1, reps = last?.reps ?: 0, weightKg = last?.weightKg ?: 0.0))
                            }
                        }) {
                            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(" Add set", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val cardio = entry.cardioData
                        Text(
                            "Cardio · ${Format.duration((cardio?.durationSeconds ?: 0).toLong())}" +
                                (cardio?.distanceKm?.let { " · ${it}km" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge, color = TextMid,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showPicker = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("  Add exercise", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = { vm.makeTemplate(sessionId) {} },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
                    ) { Text("Make template", color = MaterialTheme.colorScheme.primary) }
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
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
                onPick = { ex ->
                    val entry = vm.newEntryForExercise(ex, session.entries.size)
                        .copy(workoutSessionId = session.id)
                    draft = session.copy(entries = session.entries + entry)
                    showPicker = false
                },
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete workout?",
            message = "This permanently removes the session from your history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.delete(sessionId) { onBack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun EditSetRow(
    index: Int,
    set: StrengthSet,
    units: UnitSystem,
    onReps: (Int) -> Unit,
    onWeight: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    // null = closed, false = editing reps, true = editing weight
    var editingWeight by remember { mutableStateOf<Boolean?>(null) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Text("${index + 1}", style = Numbers, color = TextMid)
        }
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ValueCell(display = set.reps.toString(), modifier = Modifier.weight(1f)) { editingWeight = false }
            Text("×", color = TextMid)
            ValueCell(
                display = Format.weight(set.weightKg, units),
                modifier = Modifier.weight(1.25f),
            ) { editingWeight = true }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.Delete, "Remove set", tint = TextMid, modifier = Modifier.size(16.dp))
        }
    }

    when (editingWeight) {
        false -> com.lukr99.workout.ui.components.NumberPadSheet(
            title = "Set ${index + 1} · Reps",
            initial = set.reps.toDouble(),
            quickStep = 1.0,
            onValue = { onReps(it.toInt()) },
            onDismiss = { editingWeight = null },
        )
        true -> com.lukr99.workout.ui.components.NumberPadSheet(
            title = "Set ${index + 1} · Weight",
            initial = Format.toDisplay(set.weightKg, units),
            quickStep = 2.5,
            allowDecimal = true,
            unitLabel = Format.unitLabel(units),
            onValue = { onWeight(Format.toKg(it, units)) },
            onDismiss = { editingWeight = null },
        )
        null -> Unit
    }
}
