package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.creation.TemplateDraft
import com.lukr99.workout.domain.creation.TemplateExerciseDraft
import com.lukr99.workout.domain.newId
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.NoteEditorSheet
import com.lukr99.workout.ui.components.NoteLine
import com.lukr99.workout.ui.components.Tag
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/** Create or edit a workout template: name, note and ordered exercises, each with its own note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    vm: LibraryViewModel,
    templateId: String?,
    onDone: () -> Unit,
) {
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

    if (existing != null && !seeded) {
        name = existing.name
        notes = existing.notes
        rows.clear()
        rows.addAll(
            existing.exercises.sortedBy { it.sortOrder }.map {
                TemplateRow(it.id.ifBlank { newId() }, it.exerciseId, it.exerciseName, it.bodyPart, it.notes)
            },
        )
        seeded = true
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (templateId == null) "New template" else "Edit template",
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(name, { name = it }, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item(key = "notes") {
                OutlinedTextField(
                    notes, { notes = it },
                    label = { Text("Template note (optional)") },
                    placeholder = { Text("Heavy day. Keep rests to 3 minutes.") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "exercises-title") {
                Text("Exercises", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            itemsIndexed(rows, key = { _, row -> row.rowId }) { index, row ->
                TemplateRowCard(
                    index = index,
                    row = row,
                    canMoveUp = index > 0,
                    canMoveDown = index < rows.lastIndex,
                    onMoveUp = { rows.add(index - 1, rows.removeAt(index)) },
                    onMoveDown = { rows.add(index + 1, rows.removeAt(index)) },
                    onRemove = { rows.removeAt(index) },
                    onEditNote = { editingNoteFor = row.rowId },
                )
            }
            item(key = "add") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface).clickable { showPicker = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("  Add exercise", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Button(
            onClick = {
                vm.saveTemplate(
                    TemplateDraft(
                        id = templateId.orEmpty(),
                        name = name,
                        notes = notes,
                        exercises = rows.map {
                            TemplateExerciseDraft(
                                id = it.rowId,
                                exerciseId = it.exerciseId,
                                exerciseName = it.name,
                                bodyPart = it.bodyPart,
                                notes = it.notes,
                            )
                        },
                    ),
                ) { result ->
                    if (result.isValid) {
                        toast(if (templateId == null) "Template created" else "Template saved")
                        onDone()
                    } else toast(result.issues.firstOrNull()?.message ?: "Could not save")
                }
            },
            enabled = rows.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        ) { Text(if (templateId == null) "Create template" else "Save changes") }
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ExercisePicker(exercises = catalog, onPick = { ex ->
                rows.add(TemplateRow(newId(), ex.id, ex.name, ex.primaryBodyPart, notes = ""))
                showPicker = false
            })
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
}

@Composable
private fun TemplateRowCard(
    index: Int,
    row: TemplateRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEditNote: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", style = Numbers, color = EmberTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(row.name, color = MaterialTheme.colorScheme.onBackground)
                if (row.bodyPart.isNotBlank()) Tag(row.bodyPart, accent = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onEditNote) {
                Icon(
                    Icons.AutoMirrored.Rounded.NoteAdd,
                    if (row.notes.isBlank()) "Add note to ${row.name}" else "Edit note on ${row.name}",
                    tint = if (row.notes.isBlank()) EmberTheme.colors.textSecondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Rounded.ArrowUpward, "Move ${row.name} up", tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Rounded.ArrowDownward, "Move ${row.name} down", tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, "Remove ${row.name}", tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        if (row.notes.isNotBlank()) {
            NoteLine(
                Icons.AutoMirrored.Rounded.Notes,
                label = null,
                text = row.notes,
                description = "Exercise note in this template",
                onClick = onEditNote,
                modifier = Modifier.padding(start = 20.dp),
            )
        }
    }
}
