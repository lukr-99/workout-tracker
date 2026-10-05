package com.lukr99.workout.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.lukr99.workout.domain.BodyParts
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.LiveWorkoutViewModel
import com.lukr99.workout.ui.components.ConfirmDialog
import com.lukr99.workout.ui.components.ExerciseGuideSheet
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.QuickCreateSheet
import com.lukr99.workout.ui.components.SetOptionsSheet

/** Shows the one sheet or dialog [sheet] names over the live workout. [onSheet] opens another or closes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiveWorkoutSheets(
    sheet: LiveSheet?,
    session: WorkoutSession?,
    vm: LiveWorkoutViewModel,
    units: UnitSystem,
    exercises: List<Exercise>,
    catalog: Map<String, Exercise>,
    onSheet: (LiveSheet?) -> Unit,
    onEditExercise: (String) -> Unit,
    onClose: () -> Unit,
    toast: (String) -> Unit,
) {
    val close = { onSheet(null) }
    val entries = session?.entries.orEmpty()
    when (sheet) {
        null -> Unit
        LiveSheet.AddExercise -> {
            val recentIds by produceState(emptyList<String>()) { value = vm.history.recentExerciseIds() }
            val lastSets by produceState(emptyMap<String, List<StrengthSet>>(), recentIds) { value = vm.history.lastSets(recentIds.toSet()) }
            ModalBottomSheet(
                onDismissRequest = close,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ExercisePicker(
                    exercises = exercises,
                    onPick = {},
                    title = "Add exercises",
                    subtitle = "Pick one or more. They go to the end of the workout.",
                    recentIds = recentIds,
                    hint = { ex -> lastSets[ex.id]?.maxByOrNull { it.weightKg }?.let { "Last ${Format.weight(it.weightKg, units)} × ${it.reps}" } },
                    onCreate = { onSheet(LiveSheet.QuickCreate(it)) },
                    onPickMany = { picked, asSuperset ->
                        vm.addExercises(picked, asSuperset)
                        close()
                        toast(if (picked.size == 1) "${picked.single().name} added" else "${picked.size} exercises added")
                    },
                )
            }
        }
        is LiveSheet.QuickCreate -> QuickCreateSheet(
            initialName = sheet.name,
            bodyParts = BodyParts.options(catalog.values.toList()).take(15),
            onCreate = {
                vm.createAndAdd(it)
                close()
                toast("${it.name} created and added")
            },
            onDismiss = { onSheet(LiveSheet.AddExercise) },
        )
        is LiveSheet.Replace -> {
            val entry = entries.firstOrNull { it.id == sheet.entryId } ?: return close()
            val sameKind = exercises.filter { it.category == entry.entryType && it.id != entry.exerciseId }
            PickerSheet(sameKind, "Replace ${entry.exerciseSnapshotName}", close, onCreate = null) {
                vm.replaceExercise(entry.id, it)
                close()
                toast("Swapped for ${it.name}")
            }
        }
        is LiveSheet.Menu -> {
            val index = entries.indexOfFirst { it.id == sheet.entryId }
            val entry = entries.getOrNull(index) ?: return close()
            val catalogExercise = catalog[entry.exerciseId]
            ExerciseMenuSheet(
                entry = entry,
                units = units,
                options = ExerciseMenuOptions(
                    canSupersetWithPrevious = index > 0,
                    groupedWithPrevious = index > 0 && entry.supersetGroup != null && entry.supersetGroup == entries[index - 1].supersetGroup,
                    canMoveUp = index > 0,
                    canMoveDown = index < entries.lastIndex,
                    hasGuide = catalogExercise != null,
                ),
                loadOutings = { vm.history.outings(entry.exerciseId) },
                onReplace = { onSheet(LiveSheet.Replace(entry.id)) },
                onToggleSuperset = { vm.toggleSupersetWithPrevious(entry.id) },
                onMoveUp = { vm.moveEntry(entry.id, up = true) },
                onMoveDown = { vm.moveEntry(entry.id, up = false) },
                onEditNote = { onSheet(LiveSheet.Note(NoteTarget.Entry(entry.id))) },
                onShowGuide = { catalogExercise?.let { onSheet(LiveSheet.Guide(it)) } },
                onRemove = { vm.removeEntry(entry.id) },
                onDismiss = close,
            )
        }
        is LiveSheet.SetOptions -> {
            val set = entries.firstOrNull { it.id == sheet.entryId }?.strengthSets?.firstOrNull { it.id == sheet.setId }
                ?: return close()
            SetOptionsSheet(
                set = set,
                onToggleTag = { vm.toggleSetTag(sheet.entryId, sheet.setId, it) },
                onRir = { vm.setRir(sheet.entryId, sheet.setId, it) },
                onRpe = { vm.setRpe(sheet.entryId, sheet.setId, it) },
                onEditNote = { onSheet(LiveSheet.Note(NoteTarget.Set(sheet.entryId, sheet.setId))) },
                onRemove = { vm.removeSet(sheet.entryId, sheet.setId) },
                onDismiss = close,
            )
        }
        is LiveSheet.Note -> {
            if (session == null) return close()
            NoteTargetSheet(
                target = sheet.target,
                session = session,
                onSaveWorkout = vm::setWorkoutNote,
                onSaveEntry = vm::setEntryNote,
                onSaveSet = vm::setSetNote,
                onDismiss = close,
            )
        }
        is LiveSheet.Guide -> ExerciseGuideSheet(
            exercise = sheet.exercise,
            onEdit = { onEditExercise(sheet.exercise.id) },
            onDismiss = close,
        )
        is LiveSheet.Superset -> SupersetEditorSheet(
            groupId = sheet.groupId,
            entries = entries,
            onAddBefore = { vm.extendSuperset(sheet.groupId, before = true); close() },
            onAddAfter = { vm.extendSuperset(sheet.groupId, before = false); close() },
            onRemoveEntry = { vm.removeFromSuperset(it); close() },
            onUngroup = { vm.ungroupSuperset(sheet.groupId); close() },
            onDismiss = close,
        )
        LiveSheet.ConfirmFinish -> ConfirmDialog(
            title = "Finish workout?",
            message = "Empty exercises are dropped. This saves the session to your history.",
            confirmLabel = "Finish",
            onConfirm = { vm.finish { onClose(); toast("Workout saved") } },
            onDismiss = close,
        )
        LiveSheet.ConfirmDiscard -> ConfirmDialog(
            title = "Discard workout?",
            message = "This session will not be saved to your history.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { vm.discard { onClose() } },
            onDismiss = close,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    exercises: List<Exercise>,
    title: String,
    onDismiss: () -> Unit,
    onCreate: ((String) -> Unit)?,
    onPick: (Exercise) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ExercisePicker(exercises = exercises, onPick = onPick, onCreate = onCreate, title = title)
    }
}
