package com.lukr99.workout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.BodyParts
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.GuideLink
import com.lukr99.workout.domain.creation.ExerciseDraft
import com.lukr99.workout.domain.newId
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.components.BodyPartChips
import com.lukr99.workout.ui.components.ExerciseThumbnail
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.LocalToast
import com.lukr99.workout.ui.components.GymNotesSection
import com.lukr99.workout.ui.components.TrackPerGymRow
import com.lukr99.workout.ui.components.resolvedExerciseImage
import com.lukr99.workout.ui.i18n.LibraryText
import com.lukr99.workout.ui.i18n.CommonText

/**
 * Create or edit an exercise: photo, name, muscles, equipment, rest, how-to steps, a guide link and
 * your own note. Writes go through the creation service, which validates them.
 */
@Composable
fun ExerciseEditorScreen(
    vm: LibraryViewModel,
    exerciseId: String?,
    initialName: String = "",
    onDone: () -> Unit,
) {
    val toast = LocalToast.current
    val catalog by vm.catalog.collectAsState()

    val editorExerciseId = remember(exerciseId) { exerciseId ?: newId() }
    var existing by remember(exerciseId) { mutableStateOf<Exercise?>(null) }
    var loaded by remember(exerciseId) { mutableStateOf(exerciseId == null) }
    var name by remember(exerciseId, initialName) { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(ExerciseCategory.Strength) }
    var bodyPart by remember { mutableStateOf("") }
    val secondary = remember { mutableStateListOf<String>() }
    var equipment by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var trackPerLocation by remember { mutableStateOf(false) }
    var localImagePath by remember { mutableStateOf<String?>(null) }
    var removedPhotoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exerciseId) {
        if (exerciseId == null) return@LaunchedEffect
        vm.getExercise(exerciseId)?.let { found ->
            existing = found
            name = found.name
            category = found.category
            bodyPart = found.primaryBodyPart
            secondary.clear()
            secondary.addAll(found.secondaryBodyParts)
            equipment = found.equipment
            rest = found.defaultRestSeconds?.toString().orEmpty()
            instructions = found.instructions
            videoUrl = found.videoUrl.orEmpty()
            notes = found.notes
            trackPerLocation = found.trackPerLocation
            localImagePath = found.localImagePath
        }
        loaded = true
    }

    val photoPicker = rememberExercisePhotoPicker(
        onPhoto = { bytes ->
            vm.saveExercisePhoto(editorExerciseId, bytes) { result ->
                result.onSuccess {
                    localImagePath = it
                    removedPhotoPath = null
                }
                    .onFailure { toast(it.message ?: LibraryText.photoSaveFailed.now()) }
            }
        },
        onError = { message -> toast(message) },
    )
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
    val bodyPartOptions = remember(catalog, bodyPart, secondary.toList()) {
        val base = BodyParts.options(catalog)
        // Keep a typed or imported part visible as a chip even when no other exercise uses it.
        base + (listOf(bodyPart) + secondary).map(String::trim).filter(String::isNotBlank)
            .filter { part -> base.none { it.equals(part, ignoreCase = true) } }
            .distinctBy(String::lowercase)
    }
    val linkInvalid = !GuideLink.isAcceptable(videoUrl)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, CommonText.back.text(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (exerciseId == null) LibraryText.newExercise.text() else LibraryText.editTitle.text(),
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (!loaded) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(LibraryText.loading.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(LibraryText.sectionImage.text())
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(previewExercise, size = 80.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !localImagePath.isNullOrBlank() -> LibraryText.imagePersonal.text()
                            resolvedImage?.attribution != null -> resolvedImage.attribution
                            resolvedImage != null -> LibraryText.imageExercise.text()
                            else -> LibraryText.imagePlaceholder.text()
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
                            Text(LibraryText.remove.text())
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = photoPicker.takePhoto,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.CameraAlt, null)
                    Spacer(Modifier.size(6.dp))
                    Text(LibraryText.takePhoto.text())
                }
                OutlinedButton(
                    onClick = photoPicker.choosePhoto,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, null)
                    Spacer(Modifier.size(6.dp))
                    Text(LibraryText.choosePhoto.text())
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text(LibraryText.name.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(LibraryText.categoryStrength.text(), category == ExerciseCategory.Strength, { category = ExerciseCategory.Strength })
                FilterChip(LibraryText.categoryCardio.text(), category == ExerciseCategory.Cardio, { category = ExerciseCategory.Cardio })
            }

            SectionTitle(LibraryText.sectionMainMuscle.text())
            BodyPartChips(
                options = bodyPartOptions,
                isSelected = { it.equals(bodyPart, ignoreCase = true) },
                onToggle = { part ->
                    bodyPart = part
                    secondary.removeAll { it.equals(part, ignoreCase = true) }
                },
            )
            OutlinedTextField(
                bodyPart, { bodyPart = it },
                label = { Text(LibraryText.orTypeOne.text()) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(LibraryText.sectionAlsoWorks.text())
            BodyPartChips(
                options = bodyPartOptions.filterNot { it.equals(bodyPart, ignoreCase = true) },
                isSelected = { part -> secondary.any { it.equals(part, ignoreCase = true) } },
                onToggle = { part ->
                    if (!secondary.removeAll { it.equals(part, ignoreCase = true) }) secondary.add(part)
                },
            )

            OutlinedTextField(equipment, { equipment = it }, label = { Text(LibraryText.equipment.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                rest, { rest = it.filter(Char::isDigit) },
                label = { Text(LibraryText.defaultRest.text()) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(LibraryText.sectionHowTo.text())
            OutlinedTextField(
                instructions, { instructions = it },
                label = { Text(LibraryText.steps.text()) },
                supportingText = { Text(LibraryText.stepsHint.text()) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                videoUrl, { videoUrl = it },
                label = { Text(LibraryText.guideLink.text()) },
                isError = linkInvalid,
                supportingText = {
                    Text(
                        if (linkInvalid) LibraryText.linkInvalid.text() else LibraryText.linkExample.text(),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(LibraryText.sectionNote.text())
            OutlinedTextField(
                notes, { notes = it },
                label = { Text(LibraryText.noteLabel.text()) },
                placeholder = { Text(LibraryText.notePlaceholder.text()) },
                supportingText = { Text(LibraryText.noteHint.text()) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (category == ExerciseCategory.Strength) {
                SectionTitle(LibraryText.sectionGyms.text())
                TrackPerGymRow(trackPerLocation, onChange = { trackPerLocation = it })
            }

            if (exerciseId != null) {
                val gyms by vm.locations.collectAsState()
                val gymNotes by remember(exerciseId) { vm.locationNotes(exerciseId) }.collectAsState(emptyMap())
                GymNotesSection(
                    locations = gyms,
                    notes = gymNotes,
                    onSave = { locationId, text -> vm.saveLocationNote(exerciseId, locationId, text) },
                )
            }

            Spacer(Modifier.size(4.dp))
            Button(
                onClick = {
                    vm.saveExercise(
                        ExerciseDraft(
                            id = editorExerciseId,
                            name = name,
                            category = category,
                            primaryBodyPart = bodyPart,
                            secondaryBodyParts = secondary.toList(),
                            equipment = equipment,
                            notes = notes,
                            defaultRestSeconds = rest.toIntOrNull(),
                            source = existing?.source ?: ExerciseSource.Custom,
                            externalSourceId = existing?.externalSourceId,
                            isArchived = existing?.isArchived ?: false,
                            imageUrl = existing?.imageUrl,
                            imageAttribution = existing?.imageAttribution,
                            localImagePath = localImagePath,
                            instructions = instructions,
                            videoUrl = videoUrl,
                            trackPerLocation = trackPerLocation,
                            // Turning it off by hand also means "don't ask again".
                            splitPromptMuted = (existing?.splitPromptMuted ?: false) ||
                                (existing?.trackPerLocation == true && !trackPerLocation),
                        ),
                    ) { result ->
                        if (result.isValid) {
                            vm.removeExercisePhoto(removedPhotoPath)
                            toast(if (exerciseId == null) LibraryText.created.now() else LibraryText.saved.now())
                            onDone()
                        } else {
                            toast(result.issues.firstOrNull()?.message ?: LibraryText.couldNotSave.now())
                        }
                    }
                },
                enabled = !linkInvalid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (exerciseId == null) LibraryText.createButton.text() else LibraryText.saveChanges.text()) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )
}
