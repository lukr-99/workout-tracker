package com.lukr99.workout.data.backup

internal data class BackupDocument(
    val uri: String,
    val name: String,
    val lastModifiedUtcMillis: Long,
)
