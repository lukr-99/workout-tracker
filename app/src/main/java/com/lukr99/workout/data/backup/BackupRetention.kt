package com.lukr99.workout.data.backup

internal object BackupRetention {
    fun expired(documents: List<BackupDocument>, keep: Int): List<BackupDocument> =
        documents
            .filter { BackupNaming.isManagedBackup(it.name) }
            .sortedWith(
                compareByDescending<BackupDocument> { it.lastModifiedUtcMillis }
                    .thenByDescending { it.name },
            )
            .drop(keep.coerceAtLeast(1))
}
