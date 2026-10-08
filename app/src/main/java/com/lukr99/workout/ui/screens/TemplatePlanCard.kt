package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.components.BodyPartTag
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * One exercise in the template editor: name, body part, then Sets, Reps and Rest. A superset member
 * gets the orange rail; the first one says so.
 */
@Composable
internal fun TemplatePlanCard(row: TemplateRow, joinedAbove: Boolean, joinedBelow: Boolean, actions: TemplatePlanActions) {
    val colors = EmberTheme.colors
    val rail = colors.primary
    val top = if (joinedAbove) 6.dp else 18.dp
    val bottom = if (joinedBelow) 6.dp else 18.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.border, shape)
            .drawBehind { if (row.supersetGroup != null && (joinedAbove || joinedBelow)) drawRect(rail, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 8.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        if (row.supersetGroup != null && joinedBelow && !joinedAbove) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SUPERSET · DONE AS ONE ROUND",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f),
                )
                actions.onUngroup?.let { ungroup ->
                    TextButton(onClick = ungroup) { Text("Ungroup", fontWeight = FontWeight.SemiBold, color = colors.primaryText) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                actions.dragHandle.size(width = 28.dp, height = 44.dp).semantics { contentDescription = "Hold to move ${row.name}" },
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Rounded.DragIndicator, null, tint = colors.textTertiary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                BodyPartTag(row.bodyPart)
            }
            CardMenu(row, actions)
        }
        Row(Modifier.padding(top = 10.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stepper(
                label = "Sets",
                value = row.targetSets?.toString() ?: "–",
                onMinus = { actions.onChange(row.copy(targetSets = row.targetSets?.minus(1)?.takeIf { it > 0 })) },
                onPlus = { actions.onChange(row.copy(targetSets = ((row.targetSets ?: 0) + 1).coerceAtMost(20))) },
            )
            Stepper(
                label = "Reps",
                value = repsText(row),
                weight = 1.2f,
                onValueClick = actions.onEditReps,
                onMinus = { actions.onChange(row.withRepsShifted(-1)) },
                onPlus = { actions.onChange(row.withRepsShifted(1)) },
            )
            Stepper(
                label = "Rest",
                value = row.restSeconds?.let { Format.clock(it) } ?: "Auto",
                onMinus = { actions.onChange(row.copy(restSeconds = row.restSeconds?.minus(15)?.takeIf { it > 0 })) },
                onPlus = { actions.onChange(row.copy(restSeconds = ((row.restSeconds ?: 45) + 15).coerceAtMost(600))) },
            )
        }
        if (row.notes.isNotBlank()) {
            Row(
                Modifier.padding(top = 8.dp, end = 10.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = actions.onEditNote),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Notes, null, tint = colors.primaryText, modifier = Modifier.size(15.dp))
                Text(row.notes, style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            }
        }
    }
}

private fun repsText(row: TemplateRow): String = when {
    row.repsMin != null && row.repsMax != null && row.repsMin != row.repsMax -> "${row.repsMin}–${row.repsMax}"
    else -> (row.repsMax ?: row.repsMin)?.toString() ?: "–"
}

@Composable
private fun RowScope.Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    weight: Float = 1f,
    onValueClick: (() -> Unit)? = null,
) {
    val colors = EmberTheme.colors
    Column(Modifier.weight(weight)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        Row(
            Modifier.padding(top = 4.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceRaised)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepKey(Icons.Rounded.Remove, "Less ${label.lowercase()}", onMinus)
            Text(
                value,
                style = Numbers.copy(fontSize = 17.sp),
                color = colors.textPrimary,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).then(
                    if (onValueClick == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = "Set the ${label.lowercase()}", onClick = onValueClick),
                ),
            )
            StepKey(Icons.Rounded.Add, "More ${label.lowercase()}", onPlus)
        }
    }
}

@Composable
private fun StepKey(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.width(34.dp).height(44.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CardMenu(row: TemplateRow, actions: TemplatePlanActions) {
    var open by remember { mutableStateOf(false) }
    val colors = EmberTheme.colors
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, "More for ${row.name}", tint = colors.textSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            fun item(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, danger: Boolean = false, action: () -> Unit): @Composable () -> Unit = {
                DropdownMenuItem(
                    text = { Text(text, color = if (danger) colors.danger else colors.textPrimary) },
                    leadingIcon = { Icon(icon, null, tint = if (danger) colors.danger else colors.textSecondary) },
                    onClick = { open = false; action() },
                )
            }
            item(if (row.notes.isBlank()) "Add a note" else "Edit the note", Icons.AutoMirrored.Rounded.NoteAdd, action = actions.onEditNote)()
            actions.onToggleSuperset?.let { toggle ->
                val joined = row.supersetGroup != null
                item(if (joined) "Leave the superset" else "Superset with previous", if (joined) Icons.Rounded.LinkOff else Icons.Rounded.Link, action = toggle)()
            }
            actions.onMoveUp?.let { item("Move up", Icons.Rounded.ArrowUpward, action = it)() }
            actions.onMoveDown?.let { item("Move down", Icons.Rounded.ArrowDownward, action = it)() }
            item("Remove", Icons.Rounded.Delete, danger = true, action = actions.onRemove)()
        }
    }
}
