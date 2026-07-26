package com.lukr99.workout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.ExerciseThumbnail
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.LocalToast

/** Create or edit a custom exercise. Routes writes through the Phase 3 creation service (validated). */
@Composable
fun ExerciseEditorScreen(
    vm: LibraryViewModel,
    exerciseId: String?,
    onDone: () -> Unit,
) {
    val toast = LocalToast.current
    val exercises by vm.exercises.collectAsState()
    val existing = remember(exerciseId, exercises) { exercises.firstOrNull { it.id == exerciseId } }

    var seeded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.Strength) }
    var bodyPart by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    if (existing != null && !seeded) {
        name = existing.name
        category = existing.category
        bodyPart = existing.primaryBodyPart
        equipment = existing.equipment
        rest = existing.defaultRestSeconds?.toString().orEmpty()
        notes = existing.notes
        seeded = true
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (exerciseId == null) "New exercise" else "Edit exercise",
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            existing?.let { exercise ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumbnail(exercise, size = 64.dp)
                    if (!exercise.imageAttribution.isNullOrBlank()) {
                        Spacer(Modifier.size(12.dp))
                        Text(
                            exercise.imageAttribution,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Strength", category == ExerciseCategory.Strength, { category = ExerciseCategory.Strength })
                FilterChip("Cardio", category == ExerciseCategory.Cardio, { category = ExerciseCategory.Cardio })
            }
            OutlinedTextField(bodyPart, { bodyPart = it }, label = { Text("Primary body part") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                rest, { rest = it.filter(Char::isDigit) },
                label = { Text("Default rest (seconds, optional)") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.size(4.dp))
            Button(
                onClick = {
                    vm.saveExercise(
                        ExerciseDraft(
                            id = exerciseId.orEmpty(),
                            name = name,
                            category = category,
                            primaryBodyPart = bodyPart,
                            equipment = equipment,
                            notes = notes,
                            defaultRestSeconds = rest.toIntOrNull(),
                            source = existing?.source ?: com.lukr99.workout.domain.ExerciseSource.Custom,
                            externalSourceId = existing?.externalSourceId,
                            isArchived = existing?.isArchived ?: false,
                            imageUrl = existing?.imageUrl,
                            imageAttribution = existing?.imageAttribution,
                        ),
                    ) { result ->
                        if (result.isValid) {
                            toast(if (exerciseId == null) "Exercise created" else "Exercise saved")
                            onDone()
                        } else {
                            toast(result.issues.firstOrNull()?.message ?: "Could not save")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (exerciseId == null) "Create exercise" else "Save changes") }
        }
    }
}
