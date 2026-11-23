package com.lukr99.workout.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.transfer.ImportCommitResult
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.ui.DataTransferViewModel
import com.lukr99.workout.data.transfer.ExportArtifact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.lukr99.workout.ui.i18n.DataText
import com.lukr99.workout.ui.i18n.LocalLanguage

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
    val scope = rememberCoroutineScope()
    val lang = LocalLanguage.current
    val documents = rememberDocumentActions(
        onPicked = vm::previewImport,
        onSaveChosen = vm::saveExport,
        onError = vm::reportError,
    )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(DataText.title.text(), style = MaterialTheme.typography.headlineMedium)
        Text(
            DataText.intro.text(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = documents.pickFile,
                enabled = !state.isWorking,
                modifier = Modifier.testTag(DataTags.IMPORT),
            ) { Text(DataText.importFile.text()) }
            OutlinedButton(
                onClick = { saveExport(scope, vm, documents, { vm.jsonArtifact() }) },
                enabled = !state.isWorking,
                modifier = Modifier.testTag(DataTags.SAVE_JSON),
            ) { Text(DataText.saveJson.text()) }
            OutlinedButton(
                onClick = { saveExport(scope, vm, documents, { vm.csvArtifact() }) },
                enabled = !state.isWorking,
                modifier = Modifier.testTag(DataTags.SAVE_CSV),
            ) { Text(DataText.saveCsv.text()) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    shareExport(scope, vm, documents, DataText.shareBackupTitle.get(lang), { vm.jsonArtifact() })
                },
                enabled = !state.isWorking,
                modifier = Modifier.testTag(DataTags.SHARE_JSON),
            ) { Text(DataText.shareJson.text()) }
            OutlinedButton(
                onClick = {
                    shareExport(scope, vm, documents, DataText.shareCsvTitle.get(lang), { vm.csvArtifact() })
                },
                enabled = !state.isWorking,
                modifier = Modifier.testTag(DataTags.SHARE_CSV),
            ) { Text(DataText.shareCsv.text()) }
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
            Text(DataText.allDeleted.text(), color = MaterialTheme.colorScheme.primary)
        }

        DataDangerZone(
            counts = state.storeCounts,
            blocker = state.eraseBlocker,
            working = state.isWorking,
            onErase = vm::eraseAll,
        )
    }
}

@Composable
private fun commitMessage(result: ImportCommitResult): String {
    val restored = buildList {
        add(DataText.workoutsCount.text(result.insertedSessions))
        if (result.insertedRuns > 0 || result.insertedRoutes > 0) {
            add(DataText.runsCount.text(result.insertedRuns) + ", " + DataText.routesCount.text(result.insertedRoutes))
        }
        if (result.restoredPhotos > 0) add(DataText.photosCount.text(result.restoredPhotos))
        if (result.restoredSettings) add(DataText.yourSettings.text())
    }.joinToString(", ")
    return when (result.mode) {
        RestoreMode.Replace -> DataText.replacedMessage.text("restored" to restored)
        RestoreMode.Merge ->
            DataText.importedMessage.text("restored" to restored) + " " + DataText.alreadyHere.text(result.skippedSessions)
    }
}

private fun saveExport(
    scope: CoroutineScope,
    vm: DataTransferViewModel,
    documents: DocumentActions,
    build: suspend () -> ExportArtifact,
) {
    scope.launch {
        runCatching { build() }
            .onSuccess(documents.saveFile)
            .onFailure { vm.reportError(it.message ?: DataText.operationFailed.now()) }
    }
}

private fun shareExport(
    scope: CoroutineScope,
    vm: DataTransferViewModel,
    documents: DocumentActions,
    title: String,
    build: suspend () -> ExportArtifact,
) {
    scope.launch {
        runCatching { build() }
            .onSuccess { documents.shareFile(it, title) }
            .onFailure { vm.reportError(it.message ?: DataText.operationFailed.now()) }
    }
}
