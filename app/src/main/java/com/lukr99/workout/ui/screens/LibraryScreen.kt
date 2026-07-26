package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.ExerciseThumbnail
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.Tag
import com.lukr99.workout.ui.theme.TextMid

private enum class LibTab { Templates, Catalog }

/** Library surface — Templates (with editor) and the exercise Catalog (search/filter, archive). */
@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    units: UnitSystem,
    onBack: () -> Unit,
    onEditTemplate: (String) -> Unit,
    onNewTemplate: () -> Unit,
    onEditExercise: (String) -> Unit,
    onNewExercise: () -> Unit,
    onStartTemplate: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(LibTab.Templates) }
    val templates by vm.templates.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val filter by vm.filter.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Library", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { if (tab == LibTab.Templates) onNewTemplate() else onNewExercise() }) {
                Icon(Icons.Rounded.Add, "New", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("Templates", tab == LibTab.Templates, { tab = LibTab.Templates })
            FilterChip("Catalog", tab == LibTab.Catalog, { tab = LibTab.Catalog })
        }

        when (tab) {
            LibTab.Templates -> TemplateList(
                templates = templates,
                onEdit = onEditTemplate,
                onStart = onStartTemplate,
                onDelete = { vm.deleteTemplate(it) },
            )
            LibTab.Catalog -> CatalogList(
                exercises = exercises,
                searchText = filter.searchText,
                selectedBodyPart = filter.bodyPart,
                selectedCategory = filter.category,
                selectedEquipment = filter.equipment,
                includeArchived = filter.includeArchived,
                onSearch = vm::setSearch,
                onBodyPart = vm::setBodyPart,
                onCategory = vm::setCategory,
                onEquipment = vm::setEquipment,
                onIncludeArchived = vm::setIncludeArchived,
                onEdit = onEditExercise,
                onArchive = { vm.archiveExercise(it) },
                onRestore = { vm.restoreExercise(it) },
            )
        }
    }
}

@Composable
private fun TemplateList(
    templates: List<WorkoutTemplate>,
    onEdit: (String) -> Unit,
    onStart: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (templates.isEmpty()) {
        EmptyHint("No templates yet — tap + to build one.", Modifier.padding(18.dp))
        return
    }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(templates, key = { it.id }) { template ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onEdit(template.id) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        template.exercises.joinToString(", ") { it.exerciseName }.ifBlank { "No exercises" },
                        style = MaterialTheme.typography.labelSmall, color = TextMid, maxLines = 1,
                    )
                }
                IconButton(onClick = { onStart(template.id) }) {
                    Icon(Icons.Rounded.PlayArrow, "Start", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CatalogList(
    exercises: List<Exercise>,
    searchText: String,
    selectedBodyPart: String,
    selectedCategory: ExerciseCategory?,
    selectedEquipment: String,
    includeArchived: Boolean,
    onSearch: (String) -> Unit,
    onBodyPart: (String) -> Unit,
    onCategory: (ExerciseCategory?) -> Unit,
    onEquipment: (String) -> Unit,
    onIncludeArchived: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (Exercise) -> Unit,
) {
    val bodyParts = remember(exercises) {
        exercises.flatMap { listOf(it.primaryBodyPart) + it.secondaryBodyParts }
            .filter(String::isNotBlank).distinctBy(String::lowercase).sorted()
    }
    val equipmentOptions = remember(exercises) {
        exercises.flatMap { it.equipment.split(',') }.map(String::trim)
            .filter(String::isNotBlank).distinctBy(String::lowercase).sorted()
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Spacer(Modifier.size(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = TextMid, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            BasicTextField(
                value = searchText,
                onValueChange = onSearch,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onBackground)),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (searchText.isEmpty()) Text("Search exercises…", color = TextMid, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
        }
        Spacer(Modifier.size(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                "All",
                selectedCategory == null && selectedBodyPart.isBlank() && selectedEquipment.isBlank(),
                {
                    onCategory(null); onBodyPart(""); onEquipment("")
                },
            )
            FilterChip("Strength", selectedCategory == ExerciseCategory.Strength, {
                onCategory(if (selectedCategory == ExerciseCategory.Strength) null else ExerciseCategory.Strength)
            })
            FilterChip("Cardio", selectedCategory == ExerciseCategory.Cardio, {
                onCategory(if (selectedCategory == ExerciseCategory.Cardio) null else ExerciseCategory.Cardio)
            })
            bodyParts.forEach { part ->
                FilterChip(part, selectedBodyPart.equals(part, true), {
                    onBodyPart(if (selectedBodyPart.equals(part, true)) "" else part)
                })
            }
            FilterChip("Archived", includeArchived, { onIncludeArchived(!includeArchived) })
        }
        if (equipmentOptions.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("Any equipment", selectedEquipment.isBlank(), { onEquipment("") })
                equipmentOptions.forEach { item ->
                    FilterChip(item, selectedEquipment.equals(item, true), {
                        onEquipment(if (selectedEquipment.equals(item, true)) "" else item)
                    })
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        if (exercises.isEmpty()) {
            EmptyHint("No matches.")
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(exercises, key = { it.id }) { ex ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onEdit(ex.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExerciseThumbnail(ex)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                ex.name, style = MaterialTheme.typography.titleMedium,
                                color = if (ex.isArchived) TextMid else MaterialTheme.colorScheme.onBackground,
                            )
                            Text(ex.bodyPartsSummary, style = MaterialTheme.typography.labelSmall, color = TextMid)
                        }
                        if (ex.equipment.isNotBlank()) {
                            Tag(ex.equipment, accent = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.size(6.dp))
                        }
                        if (ex.isArchived) {
                            IconButton(onClick = { onRestore(ex) }) {
                                Icon(Icons.Rounded.Unarchive, "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            IconButton(onClick = { onArchive(ex.id) }) {
                                Icon(Icons.Rounded.Archive, "Archive", tint = TextMid, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
