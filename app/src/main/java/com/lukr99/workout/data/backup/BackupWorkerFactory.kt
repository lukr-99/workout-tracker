package com.lukr99.workout.data.backup

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

/**
 * Builds [BackupWorker] with its backup action injected. Any other worker class falls back to
 * WorkManager's default reflection (null).
 */
internal class BackupWorkerFactory(
    private val runBackup: suspend () -> BackupRunSummary,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == BackupWorker::class.java.name) {
            BackupWorker(appContext, workerParameters, runBackup)
        } else {
            null
        }
}
