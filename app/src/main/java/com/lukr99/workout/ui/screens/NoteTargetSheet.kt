package com.lukr99.workout.ui.screens

import com.lukr99.workout.ui.i18n.NoteSheetText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.ui.components.NoteEditorSheet

/**
 * Opens [NoteEditorSheet] for [target] inside [session], with copy that says where the note will
 * show up. Used by the live workout and by editing a past workout. If the exercise or set was
 * removed meanwhile, the sheet closes itself.
 */
@Composable
fun NoteTargetSheet(
    target: NoteTarget,
    session: WorkoutSession,
    onSaveWorkout: (String) -> Unit,
    onSaveEntry: (entryId: String, text: String) -> Unit,
    onSaveSet: (entryId: String, setId: String, text: String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (target) {
        NoteTarget.Workout -> NoteEditorSheet(
            title = NoteSheetText.workoutTitle.text(),
            initial = session.notes,
            hint = NoteSheetText.workoutHint.text(),
            onSave = onSaveWorkout,
            onDismiss = onDismiss,
        )
        is NoteTarget.Entry -> {
            val entry = session.entries.firstOrNull { it.id == target.entryId }
            if (entry == null) {
                LaunchedEffect(target) { onDismiss() }
                return
            }
            NoteEditorSheet(
                title = NoteSheetText.entryTitle.text("name" to entry.exerciseSnapshotName),
                initial = entry.notes,
                hint = NoteSheetText.entryHint.text(),
                placeholder = NoteSheetText.entryPlaceholder.text(),
                onSave = { onSaveEntry(target.entryId, it) },
                onDismiss = onDismiss,
            )
        }
        is NoteTarget.Set -> {
            val entry = session.entries.firstOrNull { it.id == target.entryId }
            val index = entry?.strengthSets?.indexOfFirst { it.id == target.setId } ?: -1
            if (entry == null || index < 0) {
                LaunchedEffect(target) { onDismiss() }
                return
            }
            NoteEditorSheet(
                title = NoteSheetText.setTitle.text("n" to index + 1),
                initial = entry.strengthSets[index].notes,
                hint = NoteSheetText.setHint.text(),
                onSave = { onSaveSet(target.entryId, target.setId, it) },
                onDismiss = onDismiss,
            )
        }
    }
}
