package com.lukr99.workout.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.PreviousEntryNote

/**
 * The notes that travel with an exercise while you log it, most lasting first:
 * the exercise's own note (set in the Library), what you wrote last time, and this workout's note.
 * Lines with nothing to say are left out, so a fresh exercise shows nothing at all.
 */
@Composable
fun ExerciseNotesPanel(
    exerciseNote: String,
    previous: PreviousEntryNote?,
    entryNote: String,
    onEditEntryNote: (() -> Unit)?,
    modifier: Modifier = Modifier,
    entryNoteLabel: String? = "Today",
) {
    // A template note is copied into every workout, so it would repeat as "Last time"; show it once.
    val previous = previous?.takeUnless { it.text.trim().equals(entryNote.trim(), ignoreCase = true) }
    if (exerciseNote.isBlank() && previous == null && entryNote.isBlank()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (exerciseNote.isNotBlank()) {
            NoteLine(Icons.Rounded.PushPin, label = null, text = exerciseNote, description = "Exercise note")
        }
        if (previous != null) {
            NoteLine(
                Icons.Rounded.History,
                label = "Last time, ${Format.shortDate(previous.atUtc)}",
                text = previous.text,
                description = "Note from last time",
            )
        }
        if (entryNote.isNotBlank()) {
            NoteLine(
                Icons.AutoMirrored.Rounded.Notes,
                label = entryNoteLabel,
                text = entryNote,
                description = "Note for this workout",
                onClick = onEditEntryNote,
            )
        }
    }
}

/** One muted line: an icon, an optional bold lead-in, then the note. Tappable when [onClick] is set. */
@Composable
fun NoteLine(
    icon: ImageVector,
    label: String?,
    text: String,
    description: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) Modifier
                else Modifier.heightIn(min = 32.dp).clickable(role = Role.Button, onClick = onClick),
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp).size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (label == null) text else "$label: $text",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
