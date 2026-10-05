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
