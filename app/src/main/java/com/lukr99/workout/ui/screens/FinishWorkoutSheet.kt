package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapVert
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.TemplateChange
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.changesIn
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.TemplateChoice
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * "Finish workout?" with the numbers so far. When the workout started from [template] and went
 * differently, it lists the changes and asks whether to update the template, keep it, or save the
 * changes as a new one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinishWorkoutSheet(
    session: WorkoutSession,
    template: WorkoutTemplate?,
    units: UnitSystem,
    volumeKg: Double,
    nowUtcMillis: Long,
    onFinish: (TemplateChoice, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    val changes = remember(session, template) { template?.changesIn(session).orEmpty() }
    var choice by remember { mutableStateOf(TemplateChoice.Update) }
    var newName by remember(template) { mutableStateOf(template?.let { "${it.name} copy" }.orEmpty()) }
    val sets = session.entries.sumOf { e -> e.strengthSets.count { it.performedAtUtc != null } }
    val prs = session.entries.sumOf { e -> e.strengthSets.count { it.isPr } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 20.dp)) {
            Text("Finish workout?", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(session.name, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(Format.duration(((nowUtcMillis - session.startedAtUtc) / 1000).coerceAtLeast(0)), "", "Time", Modifier.weight(1f))
                Stat(Format.volume(volumeKg, units), Format.unitLabel(units), "Lifted", Modifier.weight(1f))
                Stat(sets.toString(), "", "Sets", Modifier.weight(1f))
                Stat(prs.toString(), "", "PRs", Modifier.weight(1f), highlight = prs > 0)
            }
            if (template != null && changes.isNotEmpty()) {
                Text(
                    "YOU CHANGED ${template.name.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceRaised).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    changes.forEach { ChangeLine(it) }
                }
                Column(Modifier.padding(top = 12.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Update ${template.name}", "Next time starts the way you just did it.", choice == TemplateChoice.Update) { choice = TemplateChoice.Update }
                    Choice("Keep ${template.name} as it is", "Only this workout has the changes.", choice == TemplateChoice.Keep) { choice = TemplateChoice.Keep }
                    Choice("Save as a new template", "Keeps ${template.name} and adds a copy with the changes.", choice == TemplateChoice.SaveAsNew) { choice = TemplateChoice.SaveAsNew }
                }
                if (choice == TemplateChoice.SaveAsNew) {
                    val shape = RoundedCornerShape(14.dp)
                    BasicTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(shape).background(colors.surfaceRaised)
                            .border(1.dp, colors.border, shape).padding(14.dp).semantics { contentDescription = "New template name" },
                    )
                }
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Keep going", filled = false, Modifier.weight(1f), onDismiss)
                Pill("Finish", filled = true, Modifier.weight(1f)) {
                    onFinish(if (template != null && changes.isNotEmpty()) choice else TemplateChoice.Keep, newName)
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, unit: String, label: String, modifier: Modifier, highlight: Boolean = false) {
    val colors = EmberTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(colors.surfaceRaised).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Numbers.copy(fontSize = 20.sp), color = if (highlight) colors.success else colors.textPrimary, maxLines = 1)
            if (unit.isNotBlank()) Text(" $unit", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(bottom = 2.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun ChangeLine(change: TemplateChange) {
    val colors = EmberTheme.colors
    val (icon, tint, text) = when (change) {
        is TemplateChange.Added -> Triple(Icons.Rounded.Add, colors.success, "Added ${change.exerciseName}")
        is TemplateChange.Skipped -> Triple(Icons.Rounded.Remove, colors.textTertiary, "Skipped ${change.exerciseName}")
        is TemplateChange.SetCount -> Triple(
            Icons.Rounded.SwapVert,
            colors.primaryText,
            "${change.exerciseName}: ${change.done} ${if (change.done == 1) "set" else "sets"}, the template has ${change.planned}",
        )
    }
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
    }
}

@Composable
private fun Choice(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) colors.primarySoft else colors.surface)
            .border(1.5.dp, if (selected) colors.primary else colors.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .then(if (selected) Modifier.border(7.dp, colors.primary, CircleShape) else Modifier.border(2.dp, colors.border, CircleShape)),
        )
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(hint, style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
    }
}

@Composable
private fun Pill(text: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.height(54.dp).clip(shape)
            .background(if (filled) colors.primary else colors.surfaceRaised)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = if (filled) colors.onPrimary else colors.textPrimary)
    }
}
