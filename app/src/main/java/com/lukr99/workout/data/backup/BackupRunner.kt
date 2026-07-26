package com.lukr99.workout.data.backup

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class BackupRunner(
    private val exportJson: suspend () -> String,
    private val gateway: BackupGateway,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun run(treeUri: String, retentionCount: Int): BackupRunSummary {
        val timestamp = now()
        val fileName = BackupNaming.fileName(timestamp)
        gateway.write(treeUri, fileName, exportJson().toByteArray(Charsets.UTF_8))

        val expired = BackupRetention.expired(
            gateway.list(treeUri),
            retentionCount.coerceAtLeast(1),
        )
        expired.forEach { gateway.delete(it) }
        return BackupRunSummary(
            result = BackupResult.Success,
            fileName = fileName,
            deleted = expired.size,
        )
    }
}

internal object BackupNaming {
    private val formatter = DateTimeFormatter
        .ofPattern("'workout-backup-'yyyyMMdd-HHmmss-SSS'.json'")
        .withZone(ZoneOffset.UTC)

    fun fileName(utcMillis: Long): String = formatter.format(Instant.ofEpochMilli(utcMillis))

    fun isManagedBackup(name: String): Boolean =
        name.startsWith("workout-backup-") && name.endsWith(".json", ignoreCase = true)
}

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
