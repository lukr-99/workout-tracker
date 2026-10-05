package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
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
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.BodyPartTag
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.ExerciseThumbnail
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.NoteLine
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.components.SearchField
import com.lukr99.workout.ui.components.SegmentedControl
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.EmberTheme

/** Library surface — Templates (with editor) and the exercise Catalog (search/filter, archive). */
@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    units: UnitSystem,
    onEditTemplate: (String) -> Unit,
    onNewTemplate: () -> Unit,
    onEditExercise: (String) -> Unit,
    onNewExercise: (String) -> Unit,
    onStartTemplate: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(LibTab.Catalog) }
    val templates by vm.templates.collectAsState()
    val exercises by vm.exercises.collectAsState()
    val filter by vm.filter.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        ScreenHeader("Library", "${exercises.size} exercises · ${templates.size} templates") {
            RoundIconButton(
                Icons.Rounded.Add,
                if (tab == LibTab.Templates) "New template" else "New exercise",
                { if (tab == LibTab.Templates) onNewTemplate() else onNewExercise("") },
                filled = true,
            )
        }
        Spacer(Modifier.size(12.dp))
        SegmentedControl(
            options = listOf(LibTab.Catalog, LibTab.Templates),
            selected = tab,
            onSelect = { tab = it },
            label = { if (it == LibTab.Catalog) "Exercises" else "Templates" },
        )
        Spacer(Modifier.size(4.dp))

        when (tab) {
            LibTab.Templates -> TemplateList(
                templates = templates,
                onEdit = onOpenTemplate,
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
                onCreate = onNewExercise,
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(templates, key = { it.id }) { template ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(EmberTheme.colors.surface)
                    .border(1.dp, EmberTheme.colors.border, RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = "Preview ${template.name}") { onEdit(template.id) }.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        template.exercises.joinToString(", ") { it.exerciseName }.ifBlank { "No exercises" },
                        style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary, maxLines = 1,
                    )
                }
                RoundIconButton(Icons.Rounded.PlayArrow, "Start ${template.name}", { onStart(template.id) }, size = 42.dp)
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
    onCreate: (String) -> Unit,
) {
    val bodyParts = remember(exercises) {
        exercises.flatMap { listOf(it.primaryBodyPart) + it.secondaryBodyParts }
            .filter(String::isNotBlank).distinctBy(String::lowercase).sorted()
    }
    val equipmentOptions = remember(exercises) {
        exercises.flatMap { it.equipment.split(',') }.map(String::trim)
            .filter(String::isNotBlank).distinctBy(String::lowercase).sorted()
    }
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.size(8.dp))
        SearchField(value = searchText, onValueChange = onSearch, placeholder = "Search exercises")
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
                }, dot = Accents.bodyPart(part))
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyHint("No matches.")
                androidx.compose.material3.TextButton(
                    onClick = { onCreate(searchText.trim()) },
                ) {
                    Text(
                        if (searchText.isBlank()) "Create exercise"
                        else "Create “${searchText.trim()}”",
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
            ) {
                itemsIndexed(exercises, key = { _, ex -> ex.id }) { index, ex ->
                    CatalogRow(
                        ex = ex,
                        first = index == 0,
                        last = index == exercises.lastIndex,
                        onEdit = { onEdit(ex.id) },
                        onArchive = { onArchive(ex.id) },
                        onRestore = { onRestore(ex) },
                    )
                }
            }
        }
    }
}

/** One catalog exercise. Rows join into one card: the first and last round the corners. */
@Composable
private fun CatalogRow(ex: Exercise, first: Boolean, last: Boolean, onEdit: () -> Unit, onArchive: () -> Unit, onRestore: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(
        topStart = if (first) 20.dp else 0.dp, topEnd = if (first) 20.dp else 0.dp,
        bottomStart = if (last) 20.dp else 0.dp, bottomEnd = if (last) 20.dp else 0.dp,
    )
    Column(Modifier.fillMaxWidth().clip(shape).background(colors.surface)) {
        if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Edit ${ex.name}", onClick = onEdit).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExerciseThumbnail(ex)
            Column(Modifier.weight(1f)) {
                Text(
                    ex.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (ex.isArchived) colors.textSecondary else colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BodyPartTag(ex.primaryBodyPart)
                    if (ex.equipment.isNotBlank()) {
                        Text(ex.equipment, style = MaterialTheme.typography.labelLarge, color = colors.textTertiary, maxLines = 1)
                    }
                }
                if (ex.notes.isNotBlank()) {
                    NoteLine(Icons.Rounded.PushPin, label = null, text = ex.notes, description = "Exercise note", maxLines = 1)
                }
            }
            if (ex.isArchived) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Rounded.Unarchive, "Restore ${ex.name}", tint = colors.primaryText, modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Rounded.Archive, "Archive ${ex.name}", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
