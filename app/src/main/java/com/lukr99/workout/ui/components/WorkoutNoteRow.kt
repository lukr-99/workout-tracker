package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The workout's own note at the top of a session. Empty, it is a quiet invitation to add one;
 * filled, it shows the note. Tapping either opens the note sheet.
 */
@Composable
fun WorkoutNoteRow(note: String, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    if (note.isBlank()) {
        NoteLine(
            Icons.AutoMirrored.Rounded.NoteAdd,
            label = null,
            text = "Add a note for this workout",
            description = "Add workout note",
            onClick = onEdit,
            modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    } else {
        NoteLine(
            Icons.AutoMirrored.Rounded.Notes,
            label = "Workout",
            text = note,
            description = "Workout note, tap to edit",
            maxLines = 6,
            onClick = onEdit,
            modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
    }
}
