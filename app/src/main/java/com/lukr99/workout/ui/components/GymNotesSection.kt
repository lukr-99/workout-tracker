package com.lukr99.workout.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.forPicker
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * The exercise editor's machine notes: one row per place with that place's note, like "Brno gym:
 * double pulley, seat 4". Works for shared and per-gym exercises, because setups differ either way.
 * A tap opens the note sheet and saves straight away; a blank note clears it.
 */
@Composable
fun GymNotesSection(
    locations: List<Location>,
    notes: Map<String, String>,
    onSave: (locationId: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val places = locations.forPicker()
    if (places.isEmpty()) return
    val colors = EmberTheme.colors
    var editing by remember { mutableStateOf<Location?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Notes by gym",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Machine setup that differs between places, like the seat or which pulley. Shown while you log this exercise there.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        places.forEach { place ->
            val note = notes[place.id].orEmpty()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = "Edit note for ${place.name}") { editing = place },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Place, null, tint = colors.primaryText, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Text(
                        note.ifBlank { "No note" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (note.isBlank()) colors.textTertiary else colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Rounded.Edit, "Edit note for ${place.name}", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
    editing?.let { place ->
        NoteEditorSheet(
            title = "Note · ${place.name}",
            initial = notes[place.id].orEmpty(),
            hint = "Setup for ${place.name} only. Leave it empty to remove the note.",
            placeholder = "Double pulley, seat 4",
            onSave = { onSave(place.id, it) },
            onDismiss = { editing = null },
        )
    }
}
