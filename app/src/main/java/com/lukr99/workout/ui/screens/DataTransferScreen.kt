package com.lukr99.workout.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.transfer.TransferIssueSeverity
import com.lukr99.workout.ui.DataTransferViewModel
import kotlinx.coroutines.launch

/**
 * Standalone Phase 3 surface. Phase 2 can route to it from Settings without changing its data
 * flow: pick -> preview -> commit, save JSON/CSV through SAF, or share either format.
 */
@Composable
fun DataTransferScreen(
    vm: DataTransferViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.previewImport(it, it.lastPathSegment) }
    }
    val jsonSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { vm.exportJson(it) } }
    val csvSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { vm.exportCsv(it) } }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Data", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Import with a dry preview first, or export a portable backup/spreadsheet.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/csv", "text/*"))
                },
                enabled = !state.isWorking,
            ) { Text("Import file") }
            OutlinedButton(
                onClick = { jsonSaveLauncher.launch("workout-backup.json") },
                enabled = !state.isWorking,
            ) { Text("Save JSON") }
            OutlinedButton(
                onClick = { csvSaveLauncher.launch("workout-history.csv") },
                enabled = !state.isWorking,
            ) { Text("Save CSV") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val intent = vm.jsonShareIntent()
                        context.startActivity(Intent.createChooser(intent, "Share workout backup"))
                    }
                },
                enabled = !state.isWorking,
            ) { Text("Share JSON") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val intent = vm.csvShareIntent()
                        context.startActivity(Intent.createChooser(intent, "Share workout CSV"))
                    }
                },
                enabled = !state.isWorking,
            ) { Text("Share CSV") }
        }

        if (state.isWorking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }
        }
        state.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        state.preview?.let { preview ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Import preview", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${preview.summary.parsedSessions} sessions · " +
                            "${preview.summary.setCount} sets/activities · " +
                            "${preview.summary.insertedExercises} new exercises",
                    )
                    Text(
                        "${preview.summary.insertedSessions} new · " +
                            "${preview.summary.changedSessions} changed · " +
                            "${preview.summary.skippedSessions} duplicates",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (preview.summary.insertedRuns > 0 || preview.summary.insertedRoutes > 0) {
                        Text(
                            "${preview.summary.insertedRuns} runs · " +
                                "${preview.summary.insertedRoutes} routes",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            onClick = vm::commitPreview,
                            enabled = preview.canCommit && !state.isWorking,
                        ) { Text("Commit import") }
                        OutlinedButton(onClick = vm::clearPreview) { Text("Cancel") }
                    }
                }
            }
        }
        state.commitResult?.let { result ->
            val runsNote = if (result.insertedRuns > 0 || result.insertedRoutes > 0) {
                " · ${result.insertedRuns} runs · ${result.insertedRoutes} routes"
            } else {
                ""
            }
            Text(
                "Imported ${result.insertedSessions} sessions$runsNote; " +
                    "${result.skippedSessions} duplicates skipped.",
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
