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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.TemplatePreviewData
import com.lukr99.workout.ui.components.BodyPartTag
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/** A template before you start it: its note, the plan per exercise with last time's best set, then Edit or Start. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePreviewSheet(
    template: WorkoutTemplate,
    data: TemplatePreviewData,
    units: UnitSystem,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    val exercises = template.exercises.sortedBy { it.sortOrder }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 20.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(
                listOfNotNull(
                    "${exercises.size} ${if (exercises.size == 1) "exercise" else "exercises"}",
                    data.lastDoneUtc?.let { "last done ${Format.relativeDay(it).replaceFirstChar(Char::lowercase)}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            if (template.notes.isNotBlank()) {
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceRaised).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(Icons.Rounded.PushPin, null, tint = colors.primaryText, modifier = Modifier.size(16.dp))
                    Text(template.notes, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                }
            }
            Row(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                Label("Plan", Modifier.weight(1f))
                Label("Sets × reps", Modifier)
            }
            exercises.forEachIndexed { index, ex ->
                val groupAbove = index > 0 && ex.supersetGroup != null && exercises[index - 1].supersetGroup == ex.supersetGroup
                val groupBelow = index < exercises.lastIndex && ex.supersetGroup != null && exercises[index + 1].supersetGroup == ex.supersetGroup
                PlanRow(index, ex, groupAbove, groupBelow, data, units)
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(colors.surfaceRaised)
                        .border(1.dp, colors.border, RoundedCornerShape(27.dp)).clickable(role = Role.Button, onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) { Text("Edit", fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                Row(
                    Modifier.weight(2f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(colors.primary)
                        .clickable(role = Role.Button, onClick = onStart),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = colors.onPrimary)
                    Text(" Start workout", fontWeight = FontWeight.Bold, color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun PlanRow(
    index: Int,
    ex: WorkoutTemplateExercise,
    groupAbove: Boolean,
    groupBelow: Boolean,
    data: TemplatePreviewData,
    units: UnitSystem,
) {
    val colors = EmberTheme.colors
    val rail = colors.primary
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                if (groupAbove || groupBelow) {
                    val top = if (groupAbove) 0f else size.height / 2
                    val bottom = if (groupBelow) size.height else size.height / 2
                    drawRect(rail, topLeft = androidx.compose.ui.geometry.Offset(0f, top), size = Size(3.dp.toPx(), bottom - top))
                }
            }
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("${index + 1}", style = Numbers.copy(fontSize = 16.sp), color = colors.textTertiary, modifier = Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(ex.exerciseName, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            BodyPartTag(ex.bodyPart)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(planText(ex), style = Numbers.copy(fontSize = 18.sp), color = colors.textPrimary)
            data.lastSets[ex.exerciseId]?.maxByOrNull { it.weightKg }?.let {
                Text("Last ${Format.weight(it.weightKg, units)} × ${it.reps}", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
            }
        }
    }
}

/** "4 × 6–8", "3 × 10", "3 sets", or a dash when there is no plan. */
internal fun planText(ex: WorkoutTemplateExercise): String {
    val reps = when {
        ex.repsMin != null && ex.repsMax != null && ex.repsMin != ex.repsMax -> "${ex.repsMin}–${ex.repsMax}"
        else -> (ex.repsMax ?: ex.repsMin)?.toString()
    }
    return when {
        ex.targetSets != null && reps != null -> "${ex.targetSets} × $reps"
        ex.targetSets != null -> "${ex.targetSets} ${if (ex.targetSets == 1) "set" else "sets"}"
        reps != null -> "× $reps"
        else -> "–"
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = EmberTheme.colors.textSecondary,
        modifier = modifier,
    )
}
