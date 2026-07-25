package com.lukr99.workout.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.CsvExportOptions
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ExportArtifact
import com.lukr99.workout.data.transfer.ImportCommitResult
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.ImportPreview
import com.lukr99.workout.data.transfer.JsonExportOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI-ready state machine for the Phase 3 pick -> preview -> commit and save/share flows. */
class DataTransferViewModel(
    private val transfer: DataTransferService,
    private val documents: AndroidDocumentGateway,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DataTransferState())
    val state: StateFlow<DataTransferState> = mutableState.asStateFlow()

    fun previewImport(uri: Uri, fileName: String? = null, options: ImportOptions = ImportOptions()) =
        launchOperation {
            val text = documents.readText(uri)
            val preview = transfer.previewImport(text, fileName, options)
            mutableState.update { it.copy(preview = preview, commitResult = null) }
        }

    fun commitPreview() = launchOperation {
        val preview = state.value.preview ?: error("There is no import preview to commit.")
        val result = transfer.commitImport(preview)
        mutableState.update { it.copy(commitResult = result) }
    }

    fun exportJson(uri: Uri, options: JsonExportOptions = JsonExportOptions()) = launchOperation {
        val artifact = transfer.exportJson(options)
        documents.writeText(uri, artifact)
        mutableState.update { it.copy(lastExport = artifact) }
    }

    fun exportCsv(uri: Uri, options: CsvExportOptions = CsvExportOptions()) = launchOperation {
        val artifact = transfer.exportCsv(options)
        documents.writeText(uri, artifact)
        mutableState.update { it.copy(lastExport = artifact) }
    }

    suspend fun jsonShareIntent(options: JsonExportOptions = JsonExportOptions()): Intent =
        documents.shareIntent(transfer.exportJson(options))

    suspend fun csvShareIntent(options: CsvExportOptions = CsvExportOptions()): Intent =
        documents.shareIntent(transfer.exportCsv(options))

    fun clearPreview() = mutableState.update { it.copy(preview = null, commitResult = null, error = null) }

    private fun launchOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(isWorking = true, error = null) }
            runCatching { block() }
                .onFailure { error -> mutableState.update { it.copy(error = error.message ?: "Operation failed.") } }
            mutableState.update { it.copy(isWorking = false) }
        }
    }

    companion object {
        fun factory(
            transfer: DataTransferService,
            documents: AndroidDocumentGateway,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DataTransferViewModel(transfer, documents) as T
        }
    }
}

data class DataTransferState(
    val isWorking: Boolean = false,
    val preview: ImportPreview? = null,
    val commitResult: ImportCommitResult? = null,
    val lastExport: ExportArtifact? = null,
    val error: String? = null,
)
