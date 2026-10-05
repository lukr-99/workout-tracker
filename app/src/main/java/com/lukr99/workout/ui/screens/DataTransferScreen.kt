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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.transfer.ImportCommitResult
import com.lukr99.workout.data.transfer.RestoreMode
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
    // Counts and the live-session check feed the danger zone; refresh after every operation.
    LaunchedEffect(state.isWorking) { if (!state.isWorking) vm.refreshErase() }
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
            "Save JSON is a full backup: workouts, runs, routes, templates, exercises, photos and " +
                "settings. Keep it somewhere outside the phone. CSV is a spreadsheet of your workouts " +
                "and cannot restore anything. Every import shows a preview before it changes anything.",
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
            ImportPreviewCard(
                preview = preview,
                working = state.isWorking,
                onMode = vm::setRestoreMode,
                onCommit = vm::commitPreview,
                onCancel = vm::clearPreview,
            )
        }
        state.commitResult?.let { result ->
            Text(commitMessage(result), color = MaterialTheme.colorScheme.primary)
        }
        if (state.erased) {
            Text("All data deleted. Ember is back to a fresh start.", color = MaterialTheme.colorScheme.primary)
        }

        DataDangerZone(
            counts = state.storeCounts,
            blocker = state.eraseBlocker,
            working = state.isWorking,
            onErase = vm::eraseAll,
        )
    }
}

private fun commitMessage(result: ImportCommitResult): String {
    val restored = buildList {
        add("${result.insertedSessions} workouts")
        if (result.insertedRuns > 0 || result.insertedRoutes > 0) add("${result.insertedRuns} runs, ${result.insertedRoutes} routes")
        if (result.restoredPhotos > 0) add("${result.restoredPhotos} photos")
        if (result.restoredSettings) add("your settings")
    }.joinToString(", ")
    return when (result.mode) {
        RestoreMode.Replace -> "Replaced your data with the backup: $restored."
        RestoreMode.Merge -> "Imported $restored. ${result.skippedSessions} were already here."
    }
}
