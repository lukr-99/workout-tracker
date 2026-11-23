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
import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.DataText

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
            Text(DataText.previewTitle.text(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            backupLine(summary.metadata)?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                DataText.workoutsCount.text(summary.parsedSessions) + " · " +
                    DataText.setsAndActivities.text(summary.setCount) + " · " +
                    (if (replacing) DataText.exercisesCount else DataText.newExercisesCount).text(summary.insertedExercises),
            )
            if (!replacing) {
                Text(
                    DataText.sessionsNewChanged.text(
                        "added" to summary.insertedSessions,
                        "changed" to summary.changedSessions,
                        "skipped" to summary.skippedSessions,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.insertedRuns > 0 || summary.insertedRoutes > 0 || summary.photos > 0) {
                Text(
                    DataText.runsCount.text(summary.insertedRuns) + " · " +
                        DataText.routesCount.text(summary.insertedRoutes) + " · " +
                        DataText.photosCount.text(summary.photos),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (preview.plan.format == DataFormat.WorkoutJson) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(DataText.merge.text(), !replacing, { onMode(RestoreMode.Merge) })
                    FilterChip(
                        DataText.replaceEverything.text(),
                        replacing,
                        { onMode(RestoreMode.Replace) },
                        accent = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    if (replacing) {
                        DataText.replaceHint.text() + (preview.plan.replaces?.let { " ${lossLine(it)}" } ?: "")
                    } else {
                        DataText.mergeHint.text()
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
                ) { Text(if (replacing) DataText.replaceMyData.text() else DataText.importButton.text()) }
                OutlinedButton(onClick = onCancel) { Text(CommonText.cancel.text()) }
            }
        }
    }

    if (confirmReplace) {
        ConfirmDialog(
            title = DataText.replaceDialogTitle.text(),
            message = (preview.plan.replaces?.let { lossLine(it) + " " } ?: "") +
                DataText.replaceDialogMessage.text(),
            confirmLabel = DataText.replace.text(),
            destructive = true,
            onConfirm = onCommit,
            onDismiss = { confirmReplace = false },
        )
    }
}

/** "This deletes 120 workouts, 30 runs, ..." for the current store. */
@Composable
internal fun lossLine(counts: StoreCounts): String =
    DataText.lossLine.text(
        "workouts" to DataText.workoutsCount.text(counts.workouts),
        "runs" to DataText.runsCount.text(counts.runs),
        "routes" to DataText.routesCount.text(counts.routes),
        "templates" to DataText.templatesCount.text(counts.templates),
        "exercises" to DataText.exercisesCount.text(counts.exercises),
    )

@Composable
private fun backupLine(metadata: Map<String, String>): String? {
    val version = metadata["appVersion"]
    val exported = metadata["exportedAtUtc"]?.let { runCatching { kotlinx.datetime.Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
    return when {
        version != null && exported != null -> DataText.emberBackupFrom.text("version" to version, "date" to Format.fullDate(exported))
        exported != null -> DataText.backupFrom.text("date" to Format.fullDate(exported))
        else -> null
    }
}
