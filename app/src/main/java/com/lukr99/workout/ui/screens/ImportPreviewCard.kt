package com.lukr99.workout.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ImportPreview
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.data.transfer.StoreCounts
import com.lukr99.workout.data.transfer.TransferIssueSeverity
import com.lukr99.workout.ui.components.ConfirmDialog
import com.lukr99.workout.ui.components.FilterChip
import com.lukr99.workout.ui.components.Format

/**
 * The import preview: what the file holds, Merge or Replace for an Ember backup, the issues found,
 * and the commit. Replacing asks once more, naming what will be deleted.
 */
@Composable
fun ImportPreviewCard(
    preview: ImportPreview,
    working: Boolean,
    onMode: (RestoreMode) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    val summary = preview.summary
    val replacing = preview.plan.mode == RestoreMode.Replace
    var confirmReplace by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import preview", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            backupLine(summary.metadata)?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${summary.parsedSessions} workouts · ${summary.setCount} sets and activities · " +
                    "${summary.insertedExercises} ${if (replacing) "exercises" else "new exercises"}",
            )
            if (!replacing) {
                Text(
                    "${summary.insertedSessions} new · ${summary.changedSessions} changed · " +
                        "${summary.skippedSessions} already here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.insertedRuns > 0 || summary.insertedRoutes > 0 || summary.photos > 0) {
                Text(
                    "${summary.insertedRuns} runs · ${summary.insertedRoutes} routes · ${summary.photos} photos",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (preview.plan.format == DataFormat.WorkoutJson) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("Merge", !replacing, { onMode(RestoreMode.Merge) })
                    FilterChip(
                        "Replace everything",
                        replacing,
                        { onMode(RestoreMode.Replace) },
                        accent = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    if (replacing) {
                        "Deletes everything on this phone first, then restores this backup exactly as it " +
                            "was saved, settings and photos included." + (preview.plan.replaces?.let { " ${lossLine(it)}" } ?: "")
                    } else {
                        "Adds what is new and merges what matches. Nothing on this phone is removed."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (replacing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            preview.plan.issues.take(8).forEach { issue ->
                Text(
                    "• ${issue.message}",
                    color = if (issue.severity == TransferIssueSeverity.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (replacing) confirmReplace = true else onCommit() },
                    enabled = preview.canCommit && !working,
                    colors = if (replacing) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) { Text(if (replacing) "Replace my data" else "Import") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }

    if (confirmReplace) {
        ConfirmDialog(
            title = "Replace all your data?",
            message = (preview.plan.replaces?.let { lossLine(it) + " " } ?: "") +
                "The backup takes their place. This cannot be undone. Save a backup first if you are not sure.",
            confirmLabel = "Replace",
            destructive = true,
            onConfirm = onCommit,
            onDismiss = { confirmReplace = false },
        )
    }
}

/** "This deletes 120 workouts, 30 runs, ..." for the current store. */
internal fun lossLine(counts: StoreCounts): String =
    "This deletes ${counts.workouts} workouts, ${counts.runs} runs, ${counts.routes} routes, " +
        "${counts.templates} templates and ${counts.exercises} exercises."

private fun backupLine(metadata: Map<String, String>): String? {
    val version = metadata["appVersion"]
    val exported = metadata["exportedAtUtc"]?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
    return when {
        version != null && exported != null -> "Ember $version backup from ${Format.fullDate(exported)}"
        exported != null -> "Backup from ${Format.fullDate(exported)}"
        else -> null
    }
}
