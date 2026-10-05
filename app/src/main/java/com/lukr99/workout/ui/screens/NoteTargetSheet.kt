package com.lukr99.workout.ui.screens

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
            title = "Workout note",
            initial = session.notes,
            hint = "How the session felt, sleep, anything worth remembering. Shown in your history.",
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
                title = "Note · ${entry.exerciseSnapshotName}",
                initial = entry.notes,
                hint = "Next time you log this exercise, this note shows up as \"Last time\".",
                placeholder = "Grip felt off, go up 2.5 kg next time",
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
                title = "Set ${index + 1} note",
                initial = entry.strengthSets[index].notes,
                hint = "A note on just this set, like a spotter or a pause.",
                onSave = { onSaveSet(target.entryId, target.setId, it) },
                onDismiss = onDismiss,
            )
        }
    }
}
