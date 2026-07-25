package com.lukr99.workout.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A single confirmation dialog reused for the app's irreversible actions (discard/delete/finish).
 * Reserved for confirmations and one-shot picks (02-design-system.md); longer edits are full screens.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A radio-style single-choice dialog over an enum/label list. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    val on = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option); onDismiss() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = on, onClick = { onSelect(option); onDismiss() })
                        Text(
                            label(option),
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
    )
}
