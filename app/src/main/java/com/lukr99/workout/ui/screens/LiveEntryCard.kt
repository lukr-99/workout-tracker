package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.CardioEditor
import com.lukr99.workout.ui.components.ExerciseNotesPanel
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.SetColumnHeader
import com.lukr99.workout.ui.components.SetRow
import com.lukr99.workout.ui.components.BodyPartTag
import com.lukr99.workout.ui.stats
import com.lukr99.workout.ui.statsSummary
import com.lukr99.workout.ui.theme.EmberTheme

/** One exercise in the live workout: header, notes, sets (or cardio) and its start/finish action. */
@Composable
fun LiveEntryCard(
    entry: WorkoutEntry,
    units: UnitSystem,
    doneIds: Set<String>,
    /** The one set in the whole workout that is up next; it gets the orange outline. */
    currentSetId: String?,
    canGroupWithPrevious: Boolean,
    groupedWithPrevious: Boolean,
    supersetPosition: Int?,
    supersetSize: Int,
    collapsed: Boolean,
    nowUtcMillis: Long,
    notes: EntryCardNotes,
    actions: EntryCardActions,
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
                        TextButton(onClick = actions.onEditSuperset) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp))
                            Text(" Edit")
                        }
                    }
                }
            }
            EntryHeader(
                entry = entry,
                entryUnits = entryUnits,
                durationSeconds = stats.durationSeconds ?: 0,
                collapsed = collapsed,
                canGroupWithPrevious = canGroupWithPrevious,
                groupedWithPrevious = groupedWithPrevious,
                hasGuide = notes.hasGuide,
                actions = actions,
            )

            ExerciseNotesPanel(
                exerciseNote = notes.exerciseNote,
                previous = notes.previous,
                entryNote = entry.notes,
                onEditEntryNote = actions.onEditNote,
            )

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
                        color = EmberTheme.colors.textSecondary,
                    )
                }
            } else if (entry.isStrength) {
                if (entry.strengthSets.isNotEmpty()) {
                    SetColumnHeader(entryUnits)
                }
                entry.strengthSets.forEachIndexed { index, set ->
                    SetRow(
                        index = index,
                        total = entry.strengthSets.size,
                        set = set,
                        units = entryUnits,
                        done = set.id in doneIds,
                        current = set.id == currentSetId,
                        exerciseName = entry.exerciseSnapshotName,
                        previous = notes.lastTimeSets.getOrNull(index),
                        before = entry.strengthSets.getOrNull(index - 1),
                        onReps = { actions.onReps(set.id, it) },
                        onWeightKg = { actions.onWeight(set.id, it) },
                        onToggleDone = { actions.onToggleDone(set.id) },
                        onOptions = { actions.onOptions(set.id) },
                    )
                }
                TextButton(onClick = actions.onAddSet) {
                    Icon(
                        Icons.Rounded.Add,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" Add set", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                CardioEditor(
                    cardio = entry.cardioData ?: CardioEntryData(workoutEntryId = entry.id),
                    onChange = actions.onCardioChange,
                )
            }
            if (!collapsed) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (entry.notes.isBlank()) {
                        TextButton(onClick = actions.onEditNote) {
                            Icon(Icons.AutoMirrored.Rounded.NoteAdd, null, modifier = Modifier.size(18.dp))
                            Text(" Add note")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    when {
                        entry.completedAtUtc != null -> TextButton(onClick = actions.onReopen) {
                            Text("Reopen exercise")
                        }
                        entry.startedAtUtc == null -> TextButton(onClick = actions.onStart) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Text(" Start exercise")
                        }
                        else -> TextButton(onClick = actions.onFinish) {
                            Text("Finish exercise", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryHeader(
    entry: WorkoutEntry,
    entryUnits: UnitSystem,
    durationSeconds: Long,
    collapsed: Boolean,
    canGroupWithPrevious: Boolean,
    groupedWithPrevious: Boolean,
    hasGuide: Boolean,
    actions: EntryCardActions,
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
                BodyPartTag(entry.exerciseSnapshotPrimaryBodyPart)
            }
            Text(
                when {
                    entry.completedAtUtc != null -> "Finished · ${Format.duration(durationSeconds)}"
                    entry.startedAtUtc != null -> "In progress · ${Format.duration(durationSeconds)}"
                    else -> "Not started"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.completedAtUtc != null) MaterialTheme.colorScheme.primary else EmberTheme.colors.textSecondary,
            )
        }
        if (entry.isStrength) {
            TextButton(onClick = actions.onToggleWeightUnit) {
                Text(Format.unitLabel(entryUnits).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
        if (entry.completedAtUtc != null) {
            IconButton(onClick = actions.onToggleCollapsed) {
                Icon(
                    if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    if (collapsed) "Expand finished exercise" else "Collapse finished exercise",
                    tint = EmberTheme.colors.textSecondary,
                )
            }
        }
        if (hasGuide) {
            IconButton(onClick = actions.onShowGuide) {
                Icon(Icons.Rounded.Info, "How to do ${entry.exerciseSnapshotName}", tint = EmberTheme.colors.textSecondary)
            }
        }
        EntryMenu(
            hasNote = entry.notes.isNotBlank(),
            canGroupWithPrevious = canGroupWithPrevious,
            groupedWithPrevious = groupedWithPrevious,
            actions = actions,
        )
    }
}

@Composable
private fun EntryMenu(
    hasNote: Boolean,
    canGroupWithPrevious: Boolean,
    groupedWithPrevious: Boolean,
    actions: EntryCardActions,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, "More actions for this exercise", tint = EmberTheme.colors.textSecondary)
        }
        // Each item closes the menu before acting, so a removed card never keeps an open menu.
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (hasNote) "Edit note" else "Add note") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, null) },
                onClick = { open = false; actions.onEditNote() },
            )
            if (canGroupWithPrevious) {
                DropdownMenuItem(
                    text = { Text(if (groupedWithPrevious) "Ungroup from previous" else "Superset with previous") },
                    leadingIcon = { Icon(if (groupedWithPrevious) Icons.Rounded.LinkOff else Icons.Rounded.Link, null) },
                    onClick = { open = false; actions.onToggleSuperset() },
                )
            }
            DropdownMenuItem(
                text = { Text("Move up") },
                leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                onClick = { open = false; actions.onMoveUp() },
            )
            DropdownMenuItem(
                text = { Text("Move down") },
                leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                onClick = { open = false; actions.onMoveDown() },
            )
            DropdownMenuItem(
                text = { Text("Remove exercise", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; actions.onRemove() },
            )
        }
    }
}
