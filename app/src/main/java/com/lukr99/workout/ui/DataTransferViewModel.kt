package com.lukr99.workout.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.CsvExportOptions
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.JsonExportOptions
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.data.transfer.UserDataEraser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI-ready state machine for the Data screen: pick -> preview (merge or replace) -> commit, save or
 * share JSON and CSV, and "Delete all data".
 */
class DataTransferViewModel(
    private val transfer: DataTransferService,
    private val documents: AndroidDocumentGateway,
    private val eraser: UserDataEraser,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DataTransferState())
    val state: StateFlow<DataTransferState> = mutableState.asStateFlow()

    /** The picked file, kept so switching between Merge and Replace can preview it again. */
    private var pickedText: String? = null
    private var pickedName: String? = null

    fun previewImport(uri: Uri, fileName: String? = null) = launchOperation {
        val text = documents.readText(uri)
        pickedText = text
        pickedName = fileName
        preview(RestoreMode.Merge)
    }

    /** Re-plans the picked file for [mode]; nothing is written until [commitPreview]. */
    fun setRestoreMode(mode: RestoreMode) {
        if (pickedText == null || state.value.preview?.plan?.mode == mode) return
        launchOperation { preview(mode) }
    }

    fun commitPreview() = launchOperation {
        val preview = state.value.preview ?: error("There is no import preview to commit.")
        val result = transfer.commitImport(preview)
        pickedText = null
        mutableState.update { it.copy(preview = null, commitResult = result) }
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

    fun clearPreview() {
        pickedText = null
        mutableState.update { it.copy(preview = null, commitResult = null, error = null) }
    }

    /** Loads what "Delete all data" would remove and whether it is allowed right now. */
    fun refreshErase() = viewModelScope.launch {
        runCatching { transfer.currentCounts() to eraser.blocker() }
            .onSuccess { (counts, blocker) ->
                mutableState.update { it.copy(storeCounts = counts, eraseBlocker = blocker) }
            }
    }

    fun eraseAll() = launchOperation {
        eraser.eraseAll()
        pickedText = null
        mutableState.update {
            it.copy(preview = null, commitResult = null, erased = true, storeCounts = transfer.currentCounts())
        }
    }

    private suspend fun preview(mode: RestoreMode) {
        val text = pickedText ?: error("Pick a file to import first.")
        val preview = transfer.previewImport(text, pickedName, ImportOptions(mode = mode))
        mutableState.update { it.copy(preview = preview, commitResult = null, erased = false) }
    }

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
            eraser: UserDataEraser,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DataTransferViewModel(transfer, documents, eraser) as T
        }
    }
}
