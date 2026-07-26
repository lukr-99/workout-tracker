package com.lukr99.workout.data.backup

data class BackupState(
    val enabled: Boolean = false,
    val treeUri: String? = null,
    val intervalHours: Long = 24,
    val retentionCount: Int = 7,
    val lastRunUtcMillis: Long? = null,
    val lastResult: BackupResult = BackupResult.NeverRun,
    val lastMessage: String? = null,
)

enum class BackupResult {
    NeverRun,
    Success,
    Failed,
    Disabled,
}

internal data class BackupRunSummary(
    val result: BackupResult,
    val fileName: String? = null,
    val deleted: Int = 0,
    val message: String? = null,
)

internal data class BackupDocument(
    val uri: String,
    val name: String,
    val lastModifiedUtcMillis: Long,
)
