package com.lukr99.workout.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs one scheduled backup. [runBackup] comes from [BackupWorkerFactory], which the app's
 * WorkManager configuration installs, so the worker never looks the container up itself.
 */
internal class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val runBackup: suspend () -> BackupRunSummary,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = when (runBackup().result) {
        BackupResult.Success, BackupResult.Disabled -> Result.success()
        BackupResult.Failed, BackupResult.NeverRun -> Result.retry()
    }
}
