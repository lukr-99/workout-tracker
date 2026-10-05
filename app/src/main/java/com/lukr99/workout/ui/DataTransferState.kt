package com.lukr99.workout.ui

import com.lukr99.workout.data.transfer.ExportArtifact
import com.lukr99.workout.data.transfer.ImportCommitResult
import com.lukr99.workout.data.transfer.ImportPreview
import com.lukr99.workout.data.transfer.StoreCounts

/** Everything the Data screen shows. */
data class DataTransferState(
    val isWorking: Boolean = false,
    val preview: ImportPreview? = null,
    val commitResult: ImportCommitResult? = null,
    val lastExport: ExportArtifact? = null,
    val error: String? = null,
    /** What the store holds now, for the replace and delete-all warnings. */
    val storeCounts: StoreCounts? = null,
    /** Why "Delete all data" is unavailable right now, or null. */
    val eraseBlocker: String? = null,
    val erased: Boolean = false,
)
