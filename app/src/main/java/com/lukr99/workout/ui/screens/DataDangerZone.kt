package com.lukr99.workout.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.transfer.StoreCounts
import com.lukr99.workout.ui.components.TypeToConfirmDialog

/**
 * The danger zone at the end of the Data screen: "Delete all data", outlined in the danger color.
 * Disabled while a workout or run is live, with the reason as its hint.
 */
@Composable
fun DataDangerZone(
    counts: StoreCounts?,
    blocker: String?,
    working: Boolean,
    onErase: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    val danger = MaterialTheme.colorScheme.error
    Column(
        Modifier.fillMaxWidth()
            .border(BorderStroke(1.dp, danger), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Danger zone", style = MaterialTheme.typography.titleMedium, color = danger, modifier = Modifier.semantics { heading() })
        Text("Delete all data", style = MaterialTheme.typography.bodyLarge)
        Text(
            blocker ?: (
                "Removes every workout, run, route, template, custom exercise, photo and setting, " +
                    "and turns off automatic backup. Your backup files stay where they are. " +
                    "Data already in Health Connect stays there."
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (blocker != null) danger else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { confirming = true },
            enabled = blocker == null && !working,
            modifier = Modifier.testTag(DataTags.DELETE_ALL),
            border = BorderStroke(1.dp, if (blocker == null) danger else MaterialTheme.colorScheme.outline),
        ) { Text("Delete all data", color = if (blocker == null) danger else MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    if (confirming) {
        TypeToConfirmDialog(
            title = "Delete all data?",
            message = (counts?.let { lossLine(it) + " " } ?: "") +
                "Ember goes back to a fresh install. This cannot be undone.",
            word = "delete",
            confirmLabel = "Delete everything",
            onConfirm = onErase,
            onDismiss = { confirming = false },
        )
    }
}
