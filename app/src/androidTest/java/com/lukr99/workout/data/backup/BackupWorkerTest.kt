package com.lukr99.workout.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupWorkerTest {
    @After
    fun clearHook() {
        BackupWorkerTestHook.run = null
    }

    @Test
    fun doWorkReturnsSuccessForFakeSuccessfulBackup() = runTest {
        BackupWorkerTestHook.run = {
            val runner = BackupRunner(
                exportJson = { """{"exportFormatVersion":"1.2"}""" },
                gateway = FakeGateway(),
                now = { 1_700_000_000_000L },
            )
            runner.run("fake-tree", 3)
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<BackupWorker>(context).build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    private class FakeGateway : BackupGateway {
        private val documents = mutableListOf<BackupDocument>()

        override suspend fun list(treeUri: String): List<BackupDocument> = documents

        override suspend fun write(
            treeUri: String,
            fileName: String,
            bytes: ByteArray,
        ) = BackupDocument("fake:$fileName", fileName, 1).also(documents::add)

        override suspend fun delete(document: BackupDocument) {
            documents.remove(document)
        }
    }
}
