package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lukr99.workout.domain.creation.TemplateDraft
import com.lukr99.workout.domain.creation.TemplateExerciseDraft
import com.lukr99.workout.domain.newId
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.NoteEditorSheet
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.components.rememberReorderState
import com.lukr99.workout.ui.components.reorderHandle
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Create or edit a template: name, note, and the exercises with their plan (sets, rep range,
 * rest), notes and supersets. Changes only affect new workouts; logged ones stay as they were.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    vm: LibraryViewModel,
    templateId: String?,
    onDone: () -> Unit,
) {
    val colors = EmberTheme.colors
    val toast = LocalToast.current
    val templates by vm.templates.collectAsState()
    val catalog by vm.catalog.collectAsState()
    val existing = remember(templateId, templates) { templates.firstOrNull { it.id == templateId } }

    var seeded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val rows = remember { mutableStateListOf<TemplateRow>() }
    var showPicker by remember { mutableStateOf(false) }
    var editingNoteFor by remember { mutableStateOf<String?>(null) }
    var editingRepsFor by remember { mutableStateOf<String?>(null) }

    if (existing != null && !seeded) {
        name = existing.name
        notes = existing.notes
        rows.clear()
        rows.addAll(
            existing.exercises.sortedBy { it.sortOrder }.map {
                TemplateRow(
                    it.id.ifBlank { newId() }, it.exerciseId, it.exerciseName, it.bodyPart, it.notes,
                    it.targetSets, it.repsMin, it.repsMax, it.restSeconds, it.supersetGroup,
                )
            },
        )
        seeded = true
    }

    fun replaceAll(next: List<TemplateRow>) {
        rows.clear()
        rows.addAll(next)
    }

    fun save() = vm.saveTemplate(
        TemplateDraft(
            id = templateId.orEmpty(),
            name = name,
            notes = notes,
            exercises = rows.map {
                TemplateExerciseDraft(
                    id = it.rowId, exerciseId = it.exerciseId, exerciseName = it.name, bodyPart = it.bodyPart, notes = it.notes,
                    targetSets = it.targetSets, repsMin = it.repsMin, repsMax = it.repsMax, restSeconds = it.restSeconds,
                    supersetGroup = it.supersetGroup,
                )
            },
        ),
    ) { result ->
        if (result.isValid) {
            toast(if (templateId == null) "Template created" else "Template saved")
            onDone()
        } else {
            toast(result.issues.firstOrNull()?.message ?: "Could not save")
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onDone)
            Text(
                if (templateId == null) "New template" else "Edit template",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            val canSave = rows.isNotEmpty()
            Box(
                Modifier.testTag("template_save").height(44.dp).clip(RoundedCornerShape(50))
                    .background(if (canSave) colors.primary else colors.surfaceRaised)
                    .clickable(enabled = canSave, role = Role.Button) { save() }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (templateId == null) "Create" else "Save", fontWeight = FontWeight.Bold, color = if (canSave) colors.onPrimary else colors.textSecondary)
            }
        }

        val listState = rememberLazyListState()
        val reorder = rememberReorderState(
            listState,
            canMove = { key -> rows.any { it.rowId == key } },
            onMove = { from, to ->
                val a = rows.indexOfFirst { it.rowId == from }
                val b = rows.indexOfFirst { it.rowId == to }
                if (a >= 0 && b >= 0) rows.add(b, rows.removeAt(a))
            },
            onDrop = { replaceAll(TemplateRowGroups.tidy(rows)) },
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item(key = "name") { Field("Name", name, "Push day", singleLine = true) { name = it } }
            item(key = "notes") { Field("Template note", notes, "Heavy bench first. Short rests on the accessories.", singleLine = false) { notes = it } }
            item(key = "exercises-title") {
                Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "EXERCISES · ${rows.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (rows.size > 1) {
                        Text("Hold the dots to move", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                    }
                }
            }
            itemsIndexed(rows, key = { _, row -> row.rowId }) { index, row ->
                val joinedAbove = TemplateRowGroups.joinedToPrevious(rows, index)
                val joinedBelow = index < rows.lastIndex && TemplateRowGroups.joinedToPrevious(rows, index + 1)
                val dragging = reorder.draggingKey == row.rowId
                Box(
                    Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) reorder.draggingOffset else 0f
                            shadowElevation = if (dragging) 12.dp.toPx() else 0f
                        }
                        .then(if (dragging) Modifier else Modifier.animateItem())
                        .padding(top = if (joinedAbove) 4.dp else 10.dp),
                ) {
                    TemplatePlanCard(
                        row = row,
                        joinedAbove = joinedAbove,
                        joinedBelow = joinedBelow,
                        actions = TemplatePlanActions(
                            onChange = { changed -> rows[index] = changed },
                            onEditReps = { editingRepsFor = row.rowId },
                            onEditNote = { editingNoteFor = row.rowId },
                            onMoveUp = if (index > 0) ({ rows.add(index - 1, rows.removeAt(index)) }) else null,
                            onMoveDown = if (index < rows.lastIndex) ({ rows.add(index + 1, rows.removeAt(index)) }) else null,
                            onToggleSuperset = when {
                                row.supersetGroup != null -> ({ replaceAll(TemplateRowGroups.leave(rows, index)) })
                                index > 0 -> ({ replaceAll(TemplateRowGroups.joinPrevious(rows, index)) })
                                else -> null
                            },
                            onRemove = { replaceAll(TemplateRowGroups.leave(rows, index).filterIndexed { i, _ -> i != index }) },
                            onUngroup = row.supersetGroup?.let { group -> { replaceAll(TemplateRowGroups.ungroup(rows, group)) } },
                            dragHandle = Modifier.reorderHandle(reorder, row.rowId),
                        ),
                    )
                }
            }
            item(key = "add") {
                val shape = RoundedCornerShape(18.dp)
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().height(54.dp).clip(shape)
                        .border(1.5.dp, colors.primary.copy(alpha = 0.5f), shape)
                        .clickable(role = Role.Button) { showPicker = true },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = colors.primaryText, modifier = Modifier.size(20.dp))
                    Text("  Add exercises", fontWeight = FontWeight.Bold, color = colors.primaryText)
                }
            }
            item(key = "footer") {
                Text(
                    "Changes here only affect new workouts. Logged workouts stay as they were.",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
        ) {
            ExercisePicker(
                exercises = catalog,
                onPick = {},
                title = "Add exercises",
                subtitle = "They go to the end of the template with 3 sets each.",
                onPickMany = { picked, asSuperset ->
                    val group = if (asSuperset && picked.size > 1) (rows.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1 else null
                    rows.addAll(picked.map { TemplateRow(newId(), it.id, it.name, it.primaryBodyPart, notes = "", targetSets = 3, supersetGroup = group) })
                    showPicker = false
                },
            )
        }
    }

    editingNoteFor?.let { rowId ->
        val index = rows.indexOfFirst { it.rowId == rowId }
        if (index < 0) {
            editingNoteFor = null
        } else {
            NoteEditorSheet(
                title = "Note · ${rows[index].name}",
                initial = rows[index].notes,
                hint = "Copied into the workout when you start this template, like \"pause on the chest\".",
                onSave = { text -> rows[index] = rows[index].copy(notes = text) },
                onDismiss = { editingNoteFor = null },
            )
        }
    }

    editingRepsFor?.let { rowId ->
        val index = rows.indexOfFirst { it.rowId == rowId }
        if (index < 0) {
            editingRepsFor = null
        } else {
            RepRangeSheet(
                exerciseName = rows[index].name,
                initialMin = rows[index].repsMin,
                initialMax = rows[index].repsMax,
                onSave = { min, max -> rows[index] = rows[index].copy(repsMin = min, repsMax = max) },
                onDismiss = { editingRepsFor = null },
            )
        }
    }
}

@Composable
private fun Field(label: String, value: String, placeholder: String, singleLine: Boolean, onValue: (String) -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.padding(top = 12.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = colors.textSecondary)
        Box(
            Modifier.padding(top = 6.dp).fillMaxWidth().clip(shape).background(colors.surfaceRaised).border(1.dp, colors.border, shape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary)
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 2,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            )
        }
    }
}
