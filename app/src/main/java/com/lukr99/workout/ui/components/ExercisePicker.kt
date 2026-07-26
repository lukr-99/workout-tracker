package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid
import kotlinx.coroutines.delay

/**
 * The catalog picker used by both the live logging loop and past-workout editing (Phase 4 wired the
 * same sheet into `WorkoutDetailScreen`). Search by name or body part; tap to pick.
 */
@Composable
fun ExercisePicker(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onCreate: ((String) -> Unit)? = null,
    title: String = "Add exercise",
) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<ExerciseCategory?>(null) }
    var bodyPart by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(250)
        debouncedQuery = query
    }
    val bodyParts = remember(exercises) {
        exercises.flatMap { listOf(it.primaryBodyPart) + it.secondaryBodyParts }
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .sorted()
    }
    val equipmentOptions = remember(exercises) {
        exercises.flatMap { it.equipment.split(',') }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .sorted()
    }
    val filtered = remember(debouncedQuery, category, bodyPart, equipment, exercises) {
        exercises.filter { exercise ->
            val queryMatches = debouncedQuery.isBlank() ||
                exercise.name.contains(debouncedQuery, true) ||
                exercise.bodyPartsSummary.contains(debouncedQuery, true) ||
                exercise.equipment.contains(debouncedQuery, true)
            val bodyMatches = bodyPart.isBlank() ||
                exercise.primaryBodyPart.equals(bodyPart, true) ||
                exercise.secondaryBodyParts.any { it.equals(bodyPart, true) }
            val equipmentMatches = equipment.isBlank() ||
                exercise.equipment.split(',').any { it.trim().equals(equipment, true) }
            queryMatches && bodyMatches && equipmentMatches &&
                (category == null || exercise.category == category)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = TextMid, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.merge(
                    androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onBackground),
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search…", color = TextMid, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("All", category == null, { category = null })
            ExerciseCategory.entries.forEach { option ->
                FilterChip(option.name, category == option, {
                    category = option.takeUnless { category == option }
                })
            }
        }
        if (bodyParts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("Any body part", bodyPart.isBlank(), { bodyPart = "" })
                bodyParts.forEach { option ->
                    FilterChip(option, bodyPart.equals(option, true), {
                        bodyPart = option.takeUnless { bodyPart.equals(option, true) }.orEmpty()
                    })
                }
            }
        }
        if (equipmentOptions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("Any equipment", equipment.isBlank(), { equipment = "" })
                equipmentOptions.forEach { option ->
                    FilterChip(option, equipment.equals(option, true), {
                        equipment = option.takeUnless { equipment.equals(option, true) }.orEmpty()
                    })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
            if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (query.isBlank()) "No exercises match these filters."
                            else "No results for “${query.trim()}”.",
                            color = TextMid,
                        )
                        if (onCreate != null) {
                            TextButton(onClick = { onCreate(query.trim()) }) {
                                Text(
                                    if (query.isBlank()) "Create exercise"
                                    else "Create “${query.trim()}”",
                                )
                            }
                        }
                    }
                }
            }
            items(filtered, key = { it.id }) { ex ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(ex) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseThumbnail(ex)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text(ex.bodyPartsSummary, style = MaterialTheme.typography.labelSmall, color = TextMid)
                    }
                    Text(ex.category.name, style = Numbers.copy(fontSize = 11.sp), color = TextMid)
                }
            }
        }
    }
}
