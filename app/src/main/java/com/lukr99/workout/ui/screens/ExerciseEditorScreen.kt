package com.lukr99.workout.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lukr99.workout.data.images.PhotoCaptureTarget
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.domain.newId
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.ExerciseThumbnail
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.resolvedExerciseImage

/** Create or edit a custom exercise. Routes writes through the Phase 3 creation service (validated). */
@Composable
fun ExerciseEditorScreen(
    vm: LibraryViewModel,
    exerciseId: String?,
    initialName: String = "",
    onDone: () -> Unit,
) {
    val toast = LocalToast.current
    val exercises by vm.exercises.collectAsState()
    val existing = remember(exerciseId, exercises) { exercises.firstOrNull { it.id == exerciseId } }

    val editorExerciseId = remember(exerciseId) { exerciseId ?: newId() }
    var seeded by remember(exerciseId) { mutableStateOf(false) }
    var name by remember(exerciseId, initialName) { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(ExerciseCategory.Strength) }
    var bodyPart by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var localImagePath by remember { mutableStateOf<String?>(null) }
    var removedPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingCapture by remember { mutableStateOf<PhotoCaptureTarget?>(null) }

    if (existing != null && !seeded) {
        name = existing.name
        category = existing.category
        bodyPart = existing.primaryBodyPart
        equipment = existing.equipment
        rest = existing.defaultRestSeconds?.toString().orEmpty()
        notes = existing.notes
        localImagePath = existing.localImagePath
        seeded = true
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            vm.importExercisePhoto(editorExerciseId, uri) { result ->
                result.onSuccess {
                    localImagePath = it
                    removedPhotoPath = null
                }
                    .onFailure { toast(it.message ?: "Could not copy the selected photo") }
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val target = pendingCapture
        pendingCapture = null
        if (saved && target != null) {
            vm.commitCapturedPhoto(editorExerciseId, target) { result ->
                result.onSuccess {
                    localImagePath = it
                    removedPhotoPath = null
                }
                    .onFailure { toast(it.message ?: "Could not save the camera photo") }
            }
        } else {
            vm.discardPhotoCapture(target)
        }
    }
    val previewExercise = (existing ?: Exercise(
        id = editorExerciseId,
        source = ExerciseSource.Custom,
    )).copy(
        name = name,
        category = category,
        primaryBodyPart = bodyPart,
        equipment = equipment,
        localImagePath = localImagePath,
    )
    val resolvedImage = resolvedExerciseImage(previewExercise)

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
            Text(
                "Exercise image",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(previewExercise, size = 80.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !localImagePath.isNullOrBlank() -> "Personal photo"
                            resolvedImage?.attribution != null -> resolvedImage.attribution
                            resolvedImage != null -> "Exercise image"
                            else -> "Body-part placeholder"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!localImagePath.isNullOrBlank()) {
                        TextButton(onClick = {
                            removedPhotoPath = localImagePath
                            localImagePath = null
                        }) {
                            Icon(Icons.Rounded.Delete, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Remove")
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        vm.createPhotoCaptureTarget()
                            .onSuccess {
                                pendingCapture = it
                                camera.launch(it.uri)
                            }
                            .onFailure { toast(it.message ?: "Could not open the camera") }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.CameraAlt, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Take photo")
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Choose")
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
                            id = editorExerciseId,
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
                            localImagePath = localImagePath,
                        ),
                    ) { result ->
                        if (result.isValid) {
                            vm.removeExercisePhoto(removedPhotoPath)
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
