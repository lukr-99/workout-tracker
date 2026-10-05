package com.lukr99.workout.data.backup

internal data class BackupRunSummary(
    val result: BackupResult,
    val fileName: String? = null,
    val deleted: Int = 0,
    val message: String? = null,
)
