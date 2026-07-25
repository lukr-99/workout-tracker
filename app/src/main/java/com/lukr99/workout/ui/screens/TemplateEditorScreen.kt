package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.creation.TemplateDraft
import com.lukr99.workout.domain.creation.TemplateExerciseDraft
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.Tag
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

private data class PickedExercise(val exerciseId: String, val name: String, val bodyPart: String)

/** Create or edit a workout template (name + ordered exercises). Saves via the creation service. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    vm: LibraryViewModel,
    templateId: String?,
    onDone: () -> Unit,
) {
    val toast = LocalToast.current
    val templates by vm.templates.collectAsState()
    val catalog by vm.exercises.collectAsState()
    val existing = remember(templateId, templates) { templates.firstOrNull { it.id == templateId } }

    var seeded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val picked = remember { mutableStateListOf<PickedExercise>() }
    var showPicker by remember { mutableStateOf(false) }

    if (existing != null && !seeded) {
        name = existing.name
        picked.clear()
        picked.addAll(existing.exercises.sortedBy { it.sortOrder }.map { PickedExercise(it.exerciseId, it.exerciseName, it.bodyPart) })
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Text("Exercises", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            items(picked, key = { it.exerciseId }) { item ->
                val index = picked.indexOf(item)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}", style = Numbers, color = TextMid, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = MaterialTheme.colorScheme.onBackground)
                        if (item.bodyPart.isNotBlank()) Tag(item.bodyPart, accent = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { if (index > 0) picked.add(index - 1, picked.removeAt(index)) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, "Up", tint = TextMid, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { if (index < picked.lastIndex) picked.add(index + 1, picked.removeAt(index)) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, "Down", tint = TextMid, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { picked.remove(item) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Delete, "Remove", tint = TextMid, modifier = Modifier.size(16.dp))
                    }
                }
            }
            item {
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
                        exercises = picked.map { TemplateExerciseDraft(exerciseId = it.exerciseId, exerciseName = it.name, bodyPart = it.bodyPart) },
                    ),
                ) { result ->
                    if (result.isValid) {
                        toast(if (templateId == null) "Template created" else "Template saved")
                        onDone()
                    } else toast(result.issues.firstOrNull()?.message ?: "Could not save")
                }
            },
            enabled = picked.isNotEmpty(),
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
            TemplatePicker(catalog) { ex ->
                picked.add(PickedExercise(ex.id, ex.name, ex.primaryBodyPart))
                showPicker = false
            }
        }
    }
}

@Composable
private fun TemplatePicker(exercises: List<Exercise>, onPick: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises else exercises.filter { it.name.contains(query, true) || it.primaryBodyPart.contains(query, true) }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text("Add exercise", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = TextMid, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onBackground)),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner -> if (query.isEmpty()) Text("Search…", color = TextMid, style = MaterialTheme.typography.bodyLarge); inner() },
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
            items(filtered, key = { it.id }) { ex ->
                Row(Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, color = MaterialTheme.colorScheme.onBackground)
                        Text(ex.bodyPartsSummary, style = MaterialTheme.typography.labelSmall, color = TextMid)
                    }
                }
            }
        }
    }
}
