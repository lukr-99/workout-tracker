package com.lukr99.workout.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lukr99.workout.WorkoutApp

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        BackupWorkerTestHook.run?.let { testRun ->
            return when (testRun().result) {
                BackupResult.Success, BackupResult.Disabled -> Result.success()
                BackupResult.Failed, BackupResult.NeverRun -> Result.retry()
            }
        }
        val app = applicationContext as? WorkoutApp ?: return Result.failure()
        return when (app.container.backup.runScheduledBackup().result) {
            BackupResult.Success, BackupResult.Disabled -> Result.success()
            BackupResult.Failed, BackupResult.NeverRun -> Result.retry()
        }
    }
}

/** Instrumented-test seam; production never assigns this. */
internal object BackupWorkerTestHook {
    var run: (suspend () -> BackupRunSummary)? = null
}
