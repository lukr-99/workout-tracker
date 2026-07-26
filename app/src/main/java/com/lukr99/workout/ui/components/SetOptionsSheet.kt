package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.ui.theme.TextMid

/**
 * Per-set options (Phase 4): set type chips plus inline RIR / RPE editing (previously only set type
 * was editable). RIR/RPE open the shared numpad; entering 0 clears the value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetOptionsSheet(
    set: StrengthSet,
    onType: (SetType) -> Unit,
    onRir: (Double?) -> Unit,
    onRpe: (Double?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // null = closed, false = editing RIR, true = editing RPE
    var editingRpe by remember { mutableStateOf<Boolean?>(null) }

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

            Text("Type", style = MaterialTheme.typography.labelMedium, color = TextMid)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SetType.entries) { type ->
                    FilterChip(type.name, set.setType == type, onClick = { onType(type) })
                }
            }

            Text("Effort", style = MaterialTheme.typography.labelMedium, color = TextMid)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RIR", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    ValueCell(display = set.rir?.let { trim(it) } ?: "–", modifier = Modifier.fillMaxWidth()) { editingRpe = false }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RPE", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    ValueCell(display = set.rpe?.let { trim(it) } ?: "–", modifier = Modifier.fillMaxWidth()) { editingRpe = true }
                }
            }

            TextButton(onClick = { onRemove(); onDismiss() }, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 6.dp))
                Text("Remove set", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    when (editingRpe) {
        false -> NumberPadSheet(
            title = "Reps in reserve (0 clears)",
            initial = set.rir ?: 0.0,
            quickStep = 1.0,
            allowDecimal = true,
            maxValue = 10.0,
            onValue = { onRir(it.takeIf { v -> v > 0.0 }) },
            onDismiss = { editingRpe = null },
        )
        true -> NumberPadSheet(
            title = "RPE (0 clears)",
            initial = set.rpe ?: 0.0,
            quickStep = 0.5,
            allowDecimal = true,
            maxValue = 10.0,
            onValue = { onRpe(it.takeIf { v -> v > 0.0 }) },
            onDismiss = { editingRpe = null },
        )
        else -> Unit
    }
}

private fun trim(v: Double): String {
    val r = Math.round(v * 10.0) / 10.0
    return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
}
