package com.lukr99.workout.ui.screens
import androidx.compose.runtime.Composable
import com.lukr99.workout.data.transfer.ExportArtifact
@Composable fun rememberDocumentActions(
    onPicked: (name: String?, read: suspend () -> String) -> Unit,
    onSaveChosen: (artifact: ExportArtifact, write: suspend () -> Unit) -> Unit,
    onError: (String) -> Unit,
): DocumentActions = TODO()
