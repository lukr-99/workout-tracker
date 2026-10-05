package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.EmberTheme
import kotlinx.coroutines.delay

/**
 * The exercise picker for the live workout, past workouts and templates. Search, filter by kind
 * and body part, recent exercises first, and a "Create" row for a name the catalog does not have.
 *
 * With [onPickMany] it is multi-select: tap rows to tick them, then add them all at once, as a
 * superset if you like. Without it a tap picks one exercise through [onPick].
 */
@Composable
fun ExercisePicker(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onCreate: ((String) -> Unit)? = null,
    title: String = "Add exercise",
    subtitle: String? = null,
    recentIds: List<String> = emptyList(),
    hint: (Exercise) -> String? = { null },
    onPickMany: ((List<Exercise>, Boolean) -> Unit)? = null,
) {
    val colors = EmberTheme.colors
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ExerciseCategory?>(null) }
    var bodyPart by remember { mutableStateOf("") }
    val picked = remember { mutableStateListOf<Exercise>() }
    var asSuperset by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        delay(200)
        debouncedQuery = query
    }
    val bodyParts = remember(exercises) {
        exercises.map { it.primaryBodyPart.trim() }.filter(String::isNotBlank).distinctBy(String::lowercase).sorted()
    }
    val sections = remember(debouncedQuery, category, bodyPart, exercises, recentIds) {
        PickerSections.of(exercises, ExerciseFilter(searchText = debouncedQuery, bodyPart = bodyPart, category = category), recentIds)
    }

    // Fill most of the sheet so the list owns the vertical drag; a fixed list height let a fling
    // reach the sheet's drag-to-dismiss and close the picker mid-scroll.
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            SearchField(query, { query = it }, placeholder = if (onCreate != null) "Search or type a new name" else "Search exercises")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("All", category == null && bodyPart.isBlank(), { category = null; bodyPart = "" })
                ExerciseCategory.entries.forEach { option ->
                    FilterChip(option.name, category == option, { category = option.takeUnless { category == option } })
                }
                bodyParts.forEach { part ->
                    FilterChip(part, bodyPart.equals(part, true), {
                        bodyPart = part.takeUnless { bodyPart.equals(part, true) }.orEmpty()
                    }, dot = Accents.bodyPart(part))
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp)) {
            val createName = sections.createName
            if (createName != null && onCreate != null) {
                item(key = "create") { CreateRow(createName) { onCreate(createName) } }
            }
            if (sections.recent.isNotEmpty()) {
                header("Recent")
                exerciseRows(sections.recent, picked, onPickMany != null, hint, onPick)
                header("All exercises")
            }
            exerciseRows(sections.all, picked, onPickMany != null, hint, onPick)
            if (sections.recent.isEmpty() && sections.all.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (query.isBlank()) "No exercises match these filters." else "No exercise called “${query.trim()}” yet.",
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }
        }
        if (onPickMany != null) {
            AddBar(
                count = picked.size,
                asSuperset = asSuperset,
                onSuperset = { asSuperset = !asSuperset },
                onAdd = { onPickMany(picked.toList(), asSuperset && picked.size > 1) },
            )
        }
    }
}

private fun LazyListScope.header(text: String) = item(key = "header-$text") {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = EmberTheme.colors.textSecondary,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

private fun LazyListScope.exerciseRows(
    rows: List<Exercise>,
    picked: MutableList<Exercise>,
    multi: Boolean,
    hint: (Exercise) -> String?,
    onPick: (Exercise) -> Unit,
) = items(rows, key = { it.id }) { ex ->
    val colors = EmberTheme.colors
    val on = picked.any { it.id == ex.id }
    val action = if (multi) {
        Modifier.toggleable(value = on, role = Role.Checkbox) { if (on) picked.removeAll { it.id == ex.id } else picked.add(ex) }
    } else {
        Modifier.clickable(role = Role.Button) { onPick(ex) }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(action).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExerciseThumbnail(ex)
        Column(Modifier.weight(1f)) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BodyPartTag(ex.primaryBodyPart)
                hint(ex)?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = colors.textTertiary, maxLines = 1) }
            }
        }
        if (multi) {
            val circle = Modifier.size(28.dp).clip(CircleShape)
            if (on) {
                Box(circle.background(colors.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            } else {
                Box(circle.border(2.dp, colors.border, CircleShape))
            }
        }
    }
}

@Composable
private fun CreateRow(name: String, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(colors.primarySoft)
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Add, null, tint = colors.primaryText)
        Column {
            Text("Create “$name”", fontWeight = FontWeight.Bold, color = colors.primaryText)
            Text("A quick form. You stay in the workout.", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
    }
}

@Composable
private fun AddBar(count: Int, asSuperset: Boolean, onSuperset: () -> Unit, onAdd: () -> Unit) {
    val colors = EmberTheme.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (count > 1) {
                val shape = RoundedCornerShape(27.dp)
                Row(
                    Modifier.height(54.dp).clip(shape)
                        .background(if (asSuperset) colors.primarySoft else colors.surfaceRaised)
                        .border(1.dp, if (asSuperset) colors.primary else colors.border, shape)
                        .toggleable(value = asSuperset, role = Role.Switch) { onSuperset() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Rounded.Layers, null, tint = if (asSuperset) colors.primaryText else colors.textPrimary, modifier = Modifier.size(18.dp))
                    Text("As a superset", fontWeight = FontWeight.Bold, color = if (asSuperset) colors.primaryText else colors.textPrimary)
                }
            }
            Box(
                Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp))
                    .background(if (count == 0) colors.surfaceRaised else colors.primary)
                    .clickable(enabled = count > 0, role = Role.Button, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (count == 0) "Pick exercises" else "Add $count",
                    fontWeight = FontWeight.Bold,
                    color = if (count == 0) colors.textSecondary else colors.onPrimary,
                )
            }
        }
    }
}
