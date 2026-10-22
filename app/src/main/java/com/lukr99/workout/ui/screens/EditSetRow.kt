package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.SetNoteLine
import com.lukr99.workout.ui.components.SetTagChips
import com.lukr99.workout.ui.components.ValueCell
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/** One set of a past workout: reps and weight open the number pad, plus a note and remove. */
@Composable
internal fun EditSetRow(
    index: Int,
    set: StrengthSet,
    units: UnitSystem,
    onReps: (Int) -> Unit,
    onWeight: (Double) -> Unit,
    onRemove: () -> Unit,
    onNote: () -> Unit,
) {
    // null = closed, false = editing reps, true = editing weight
    var editingWeight by remember { mutableStateOf<Boolean?>(null) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Text("${index + 1}", style = Numbers, color = EmberTheme.colors.textSecondary)
        }
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ValueCell(display = set.reps.toString(), modifier = Modifier.weight(1f)) { editingWeight = false }
            Text("×", color = EmberTheme.colors.textSecondary)
            ValueCell(
                display = Format.weight(set.weightKg, units),
                modifier = Modifier.weight(1.25f),
            ) { editingWeight = true }
        }
        IconButton(onClick = onNote, modifier = Modifier.size(38.dp)) {
            Icon(
                Icons.AutoMirrored.Rounded.NoteAdd,
                if (set.notes.isBlank()) "Add note to set ${index + 1}" else "Edit note on set ${index + 1}",
                tint = if (set.notes.isBlank()) EmberTheme.colors.textSecondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Rounded.Delete, "Remove set", tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
    SetTagChips(set, Modifier.fillMaxWidth().padding(start = 48.dp, top = 3.dp))
    SetNoteLine(set)

    when (editingWeight) {
        false -> com.lukr99.workout.ui.components.NumberPadSheet(
            title = "Set ${index + 1} · Reps",
            initial = set.reps.toDouble(),
            quickStep = 1.0,
            onValue = { onReps(it.toInt()) },
            onDismiss = { editingWeight = null },
        )
        true -> com.lukr99.workout.ui.components.NumberPadSheet(
            title = "Set ${index + 1} · Weight",
            initial = Format.toDisplay(set.weightKg, units),
            quickStep = 2.5,
            allowDecimal = true,
            unitLabel = Format.unitLabel(units),
            onValue = { onWeight(Format.toKg(it, units)) },
            onDismiss = { editingWeight = null },
        )
        null -> Unit
    }
}
