package com.lukr99.workout.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * The set number pad: weight and reps side by side, quick steps, "same as the set before", a
 * keypad, then Next or Done set. Values reach the set live through [onChange], so the row behind
 * the sheet updates while you type. [onDone] marks the set done; the caller closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEntrySheet(
    title: String,
    subtitle: String,
    initial: SetEntryState,
    unitLabel: String,
    weightStep: Double,
    copyLabel: String?,
    copyFrom: Pair<Double, Int>?,
    onChange: (weightDisplay: Double, reps: Int) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    var state by remember { mutableStateOf(initial) }
    fun update(next: SetEntryState) {
        val changed = next.weight != state.weight || next.reps != state.reps
        state = next
        if (changed) onChange(next.weightValue, next.repsValue)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldCell("Weight", state.weight, unitLabel, state.field == SetField.Weight) { state = state.select(SetField.Weight) }
                FieldCell("Reps", state.reps, null, state.field == SetField.Reps) { state = state.select(SetField.Reps) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val steps = if (state.field == SetField.Weight) listOf(-weightStep, weightStep, weightStep * 2) else listOf(-1.0, 1.0, 2.0)
                val unit = if (state.field == SetField.Weight) " $unitLabel" else ""
                steps.forEach { delta ->
                    val text = (if (delta > 0) "+" else "−") + SetEntryState.trim(kotlin.math.abs(delta)) + unit
                    StepButton(text) { update(state.step(delta)) }
                }
                if (copyLabel != null && copyFrom != null) {
                    StepButton(copyLabel, icon = true, weight = 1.4f) { update(state.copyFrom(copyFrom.first, copyFrom.second)) }
                }
            }
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", SetEntryState.BACKSPACE))
                .forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key -> Key(key, enabled = !(key == "." && state.field == SetField.Reps)) { update(state.press(key)) } }
                    }
                }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val toReps = state.field == SetField.Weight
                Box(
                    Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(colors.surfaceRaised)
                        .border(1.dp, colors.border, RoundedCornerShape(27.dp))
                        .clickable(role = Role.Button) { state = state.select(if (toReps) SetField.Reps else SetField.Weight) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (toReps) "Next: reps" else "Back to weight", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Row(
                    Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(colors.primary)
                        .clickable(role = Role.Button, onClick = onDone),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
                    Text("  Done set", fontWeight = FontWeight.Bold, color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun RowScope.FieldCell(label: String, value: String, unit: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.weight(1f).clip(shape)
            .background(if (selected) colors.primarySoft else colors.surfaceRaised)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.border, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = "$label ${value}${unit?.let { " $it" }.orEmpty()}" }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Numbers.copy(fontSize = 38.sp), color = colors.textPrimary)
            if (unit != null) Text(" $unit", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun RowScope.StepButton(text: String, icon: Boolean = false, weight: Float = 1f, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Row(
        Modifier.weight(weight).height(44.dp).clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) Icon(Icons.Rounded.ContentCopy, null, tint = colors.primaryText, modifier = Modifier.size(15.dp).padding(end = 2.dp))
        Text(
            text,
            style = if (icon) MaterialTheme.typography.labelLarge else Numbers.copy(fontSize = 16.sp),
            fontWeight = FontWeight.Bold,
            color = if (icon) colors.primaryText else colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.Key(key: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Box(
        Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(colors.surfaceRaised)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == SetEntryState.BACKSPACE) {
            Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete", tint = colors.textPrimary)
        } else {
            Text(key, style = Numbers.copy(fontSize = 26.sp), color = if (enabled) colors.textPrimary else colors.textTertiary)
        }
    }
}
