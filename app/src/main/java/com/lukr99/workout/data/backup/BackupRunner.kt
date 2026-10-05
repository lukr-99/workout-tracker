package com.lukr99.workout.data.backup

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
