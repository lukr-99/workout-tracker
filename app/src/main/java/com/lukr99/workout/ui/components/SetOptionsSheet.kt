package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.MAX_REPS_IN_RESERVE
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.effectiveTags
import com.lukr99.workout.domain.repsInReserve
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Per-set options: tags, effort as "reps left in the tank" chips (tap the chosen one again to clear
 * it), the set's note, and Duplicate or Remove.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetOptionsSheet(
    set: StrengthSet,
    onToggleTag: (SetTag) -> Unit,
    onRepsInReserve: (Int?) -> Unit,
    onEditNote: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Set options", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)

            Text("Tags · choose any that apply", style = MaterialTheme.typography.labelMedium, color = EmberTheme.colors.textSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SetTag.entries) { tag ->
                    FilterChip(tag.label, tag in set.effectiveTags, onClick = { onToggleTag(tag) })
                }
            }

            Text("Reps left in the tank", style = MaterialTheme.typography.labelMedium, color = EmberTheme.colors.textSecondary)
            val reserve = set.repsInReserve
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..MAX_REPS_IN_RESERVE).forEach { reps ->
                    val label = if (reps == MAX_REPS_IN_RESERVE) "$reps+" else "$reps"
                    FilterChip(label, reps == reserve, onClick = { onRepsInReserve(if (reps == reserve) null else reps) })
                }
            }
            Text(
                when (reserve) {
                    null -> "How many more reps you could have done. Leave it empty if you did not track it."
                    MAX_REPS_IN_RESERVE -> "Same as RPE ${10 - reserve} or easier. Tap it again to clear."
                    else -> "Same as RPE ${10 - reserve}. Tap it again to clear."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = EmberTheme.colors.textSecondary,
            )

            Text("Note", style = MaterialTheme.typography.labelMedium, color = EmberTheme.colors.textSecondary)
            NoteLine(
                if (set.notes.isBlank()) Icons.AutoMirrored.Rounded.NoteAdd else Icons.AutoMirrored.Rounded.Notes,
                label = null,
                text = set.notes.ifBlank { "Add a note to this set" },
                description = if (set.notes.isBlank()) "Add set note" else "Edit set note",
                onClick = onEditNote,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onDuplicate(); onDismiss() }) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.padding(end = 6.dp))
                    Text("Duplicate")
                }
                TextButton(onClick = { onRemove(); onDismiss() }) {
                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 6.dp))
                    Text("Remove set", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

val SetTag.label: String
    get() = when (this) {
        SetTag.Warmup -> "Warm-up"
        SetTag.Drop -> "Drop"
        SetTag.ToFailure -> "To failure"
        SetTag.Failed -> "Failed early"
        SetTag.Negative -> "Negative"
        SetTag.BackOff -> "Back-off"
    }
