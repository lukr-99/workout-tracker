package com.lukr99.workout.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.lukr99.workout.data.transfer.ExportArtifact
import com.lukr99.workout.web.platform.downloadText
import com.lukr99.workout.web.platform.isIosDevice
import com.lukr99.workout.web.platform.pickFile
import com.lukr99.workout.web.platform.pickedFileName
import com.lukr99.workout.web.platform.readPickedFileText
import com.lukr99.workout.web.platform.shareTextFile
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

private const val ACCEPTED_FILES = ".json,.csv,application/json,text/csv"

/**
 * The file actions for the Data screen, over the browser.
 *
 * - Import opens the browser's file chooser and reads the file as text.
 * - Save downloads the export. On an iPhone it first offers the share sheet with the file, which is
 *   the only way to put a file into Files there, and falls back to a download.
 * - Share offers the share sheet with the file, or a download when the browser cannot share files.
 *
 * Each platform supplies this file.
 */
@Composable
fun rememberDocumentActions(
    onPicked: (name: String?, read: suspend () -> String) -> Unit,
    onSaveChosen: (artifact: ExportArtifact, write: suspend () -> Unit) -> Unit,
    onError: (String) -> Unit,
): DocumentActions {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val saveChosen by rememberUpdatedState(onSaveChosen)
    val failed by rememberUpdatedState(onError)

    return remember(scope) {
        DocumentActions(
            pickFile = {
                // The chooser has to open inside the tap, so the promise starts here and only the wait is launched.
                val chosen = runCatching { pickFile(ACCEPTED_FILES) }
                scope.launch {
                    chosen.mapCatching { it.await<JsAny?>() }
                        .onSuccess { file ->
                            if (file != null) picked(pickedFileName(file)) { readPickedFileText(file).await<JsString>().toString() }
                        }
                        .onFailure { failed("Could not open the file chooser.") }
                }
            },
            saveFile = { artifact ->
                saveChosen(artifact) {
                    val handled = isIosDevice() && offerToShare(artifact, "Save ${artifact.fileName}")
                    if (!handled) downloadText(artifact.fileName, artifact.text, artifact.mimeType)
                }
            },
            shareFile = { artifact, title ->
                scope.launch {
                    runCatching {
                        if (!offerToShare(artifact, title)) downloadText(artifact.fileName, artifact.text, artifact.mimeType)
                    }.onFailure { failed(it.message ?: "Could not open the share sheet") }
                }
            },
        )
    }
}

/**
 * Shows the share sheet with [artifact] as a file. True when that is settled (it was shared, or the
 * person closed the sheet); false when the browser cannot share the file, so the caller downloads it.
 */
private suspend fun offerToShare(artifact: ExportArtifact, title: String): Boolean =
    when (shareTextFile(artifact.fileName, artifact.text, artifact.mimeType, title).await<JsString>().toString()) {
        "shared", "cancelled" -> true
        else -> false
    }
