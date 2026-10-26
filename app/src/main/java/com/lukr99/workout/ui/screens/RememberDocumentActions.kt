package com.lukr99.workout.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.ExportArtifact
import kotlinx.coroutines.launch

/**
 * The file actions for the Data screen.
 *
 * - [onPicked] gets the picked file's name and a function that reads its text; the caller runs the
 *   read inside its own busy and error handling.
 * - [onSaveChosen] gets the export and a function that writes it to the place the person chose.
 * - [onError] gets a message when the share sheet could not be opened.
 *
 * Each platform supplies this file; the web edition's is under web/.
 */
@Composable
fun rememberDocumentActions(
    onPicked: (name: String?, read: suspend () -> String) -> Unit,
    onSaveChosen: (artifact: ExportArtifact, write: suspend () -> Unit) -> Unit,
    onError: (String) -> Unit,
): DocumentActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gateway = remember(context) { AndroidDocumentGateway(context) }
    var pendingSave by remember { mutableStateOf<ExportArtifact?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(it.lastPathSegment) { gateway.readText(it) } }
    }
    val jsonSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> saveTo(uri, pendingSave, gateway, onSaveChosen) }
    val csvSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> saveTo(uri, pendingSave, gateway, onSaveChosen) }

    return remember(context, gateway) {
        DocumentActions(
            pickFile = { importLauncher.launch(arrayOf("application/json", "text/csv", "text/*")) },
            saveFile = { artifact ->
                pendingSave = artifact
                // The two launchers only differ in the MIME type they ask the system for.
                if (artifact.mimeType == "text/csv") csvSaveLauncher.launch(artifact.fileName)
                else jsonSaveLauncher.launch(artifact.fileName)
            },
            shareFile = { artifact, title ->
                scope.launch {
                    runCatching {
                        val intent = gateway.shareIntent(artifact)
                        context.startActivity(android.content.Intent.createChooser(intent, title))
                    }.onFailure { onError(it.message ?: "Could not open the share sheet") }
                }
            },
        )
    }
}

private fun saveTo(
    uri: android.net.Uri?,
    artifact: ExportArtifact?,
    gateway: AndroidDocumentGateway,
    onSaveChosen: (ExportArtifact, suspend () -> Unit) -> Unit,
) {
    if (uri != null && artifact != null) onSaveChosen(artifact) { gateway.writeText(uri, artifact) }
}
