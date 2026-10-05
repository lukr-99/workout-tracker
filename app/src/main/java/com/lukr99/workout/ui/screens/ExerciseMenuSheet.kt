package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.ExerciseOuting
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.BodyPartTag
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * The menu behind an exercise card's three dots: the last three times you did it, then what you
 * can do with it now. Replace keeps the logged sets, for when the machine is taken. Each action
 * closes the sheet before it runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseMenuSheet(
    entry: WorkoutEntry,
    units: UnitSystem,
    options: ExerciseMenuOptions,
    loadOutings: suspend () -> List<ExerciseOuting>,
    onReplace: () -> Unit,
    onToggleSuperset: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditNote: () -> Unit,
    onShowGuide: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    var outings by remember { mutableStateOf<List<ExerciseOuting>?>(null) }
    LaunchedEffect(entry.exerciseId) { outings = runCatching { loadOutings() }.getOrDefault(emptyList()) }

    fun act(action: () -> Unit): () -> Unit = { onDismiss(); action() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 20.dp),
        ) {
            Text(entry.exerciseSnapshotName, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            BodyPartTag(entry.exerciseSnapshotPrimaryBodyPart)
            if (entry.isStrength) {
                History(outings, units)
            }
            Column(Modifier.padding(top = 8.dp)) {
                if (entry.isStrength) {
                    Action(Icons.Rounded.SwapHoriz, "Replace exercise", "Machine taken? Swap it and keep your logged sets.", onClick = act(onReplace))
                }
                if (options.canSupersetWithPrevious) {
                    Action(
                        if (options.groupedWithPrevious) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                        if (options.groupedWithPrevious) "Ungroup from previous" else "Superset with previous",
                        null,
                        onClick = act(onToggleSuperset),
                    )
                }
                if (options.canMoveUp) Action(Icons.Rounded.ArrowUpward, "Move up", null, onClick = act(onMoveUp))
                if (options.canMoveDown) Action(Icons.Rounded.ArrowDownward, "Move down", null, onClick = act(onMoveDown))
                Action(Icons.AutoMirrored.Rounded.NoteAdd, if (entry.notes.isBlank()) "Add a note for today" else "Edit today's note", null, onClick = act(onEditNote))
                if (options.hasGuide) Action(Icons.Rounded.Info, "How to do it", "Steps, your note and the guide link.", onClick = act(onShowGuide))
                val done = entry.strengthSets.count { it.performedAtUtc != null }
                Action(
                    Icons.Rounded.Delete,
                    "Remove from workout",
                    if (done > 0) "Its $done done ${if (done == 1) "set goes" else "sets go"} too." else null,
                    danger = true,
                    onClick = act(onRemove),
                )
            }
        }
    }
}

@Composable
private fun History(outings: List<ExerciseOuting>?, units: UnitSystem) {
    val colors = EmberTheme.colors
    Text(
        "LAST 3 TIMES",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceRaised).padding(horizontal = 12.dp, vertical = 4.dp)) {
        when {
            outings == null -> Text("Loading…", color = colors.textSecondary, modifier = Modifier.padding(vertical = 10.dp))
            outings.isEmpty() -> Text("First time. Nothing logged yet.", color = colors.textSecondary, modifier = Modifier.padding(vertical = 10.dp))
            else -> outings.forEachIndexed { index, outing ->
                if (index > 0) Box(Modifier.fillMaxWidth().padding(vertical = 0.dp).heightIn(min = 1.dp).background(colors.border))
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Format.shortDate(outing.atUtc), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, modifier = Modifier.width(72.dp))
                    Text(
                        outing.sets.joinToString(", ") { "${Format.weight(it.weightKg, units)} × ${it.reps}" },
                        style = Numbers.copy(fontSize = 15.sp),
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(Format.weight(outing.bestE1rmKg, units), style = Numbers.copy(fontSize = 15.sp), color = colors.violet)
                }
            }
        }
    }
    if (!outings.isNullOrEmpty()) {
        Text(
            "Right column: best estimated 1RM (${Format.unitLabel(units)})",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, hint: String?, danger: Boolean = false, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val tint = if (danger) colors.danger else colors.textPrimary
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceRaised), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = tint)
            if (hint != null) Text(hint, style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
    }
}
