package com.lukr99.workout.data.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRunnerTest {
    @Test
    fun namingUsesStableUtcFormat() {
        assertEquals(
            "workout-backup-20231114-221320-000.json",
            BackupNaming.fileName(1_700_000_000_000L),
        )
        assertTrue(BackupNaming.isManagedBackup("workout-backup-20231114-221320-000.json"))
        assertFalse(BackupNaming.isManagedBackup("other.json"))
    }

    @Test
    fun runWritesJsonAndDeletesOnlyExpiredManagedBackups() = runTest {
        val gateway = FakeBackupGateway(
            mutableListOf(
                BackupDocument("old", "workout-backup-20230101-000000-000.json", 100),
                BackupDocument("newer", "workout-backup-20230201-000000-000.json", 200),
                BackupDocument("manual", "notes.json", 1),
            ),
        )
        val runner = BackupRunner(
            exportJson = { """{"exportFormatVersion":"1.2"}""" },
            gateway = gateway,
            now = { 1_700_000_000_000L },
        )

        val result = runner.run("tree", retentionCount = 2)

        assertEquals(BackupResult.Success, result.result)
        assertEquals(1, result.deleted)
        assertEquals(listOf("old"), gateway.deleted)
        assertTrue(gateway.lastBytes.decodeToString().contains("\"1.2\""))
    }

    private class FakeBackupGateway(
        private val documents: MutableList<BackupDocument>,
    ) : BackupGateway {
        val deleted = mutableListOf<String>()
        var lastBytes = byteArrayOf()

        override suspend fun list(treeUri: String): List<BackupDocument> = documents

        override suspend fun write(
            treeUri: String,
            fileName: String,
            bytes: ByteArray,
        ): BackupDocument = BackupDocument("written", fileName, 300).also {
            lastBytes = bytes
            documents += it
        }

        override suspend fun delete(document: BackupDocument) {
            deleted += document.uri
            documents.remove(document)
        }
    }
}
