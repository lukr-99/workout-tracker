package com.lukr99.workout.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupWorkerTest {

    @Test
    fun doWorkReturnsSuccessForFakeSuccessfulBackup() = runTest {
        val runner = BackupRunner(
            exportJson = { """{"exportFormatVersion":"1.2"}""" },
            gateway = FakeGateway(),
            now = { 1_700_000_000_000L },
        )
        val worker = workerRunning { runner.run("fake-tree", 3) }

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun doWorkRetriesAFailedBackup() = runTest {
        val worker = workerRunning { BackupRunSummary(result = BackupResult.Failed) }

        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
    }

    @Test
    fun theFactoryBuildsTheBackupWorker() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorkerFactory { BackupRunSummary(result = BackupResult.Success) })
            .build()
        assertTrue(worker is BackupWorker)
    }

    @Test
    fun workManagerStartsFromTheAppsOwnConfiguration() {
        // The manifest removes WorkManager's default initializer, so this only works when WorkoutApp
        // provides the configuration (with the factory that injects the backup into the worker).
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context is Configuration.Provider)
        WorkManager.getInstance(context)
    }

    private fun workerRunning(run: suspend () -> BackupRunSummary): BackupWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorkerFactory(run))
            .build()
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
