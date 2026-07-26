package com.lukr99.workout.data.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backupDataStore by preferencesDataStore(name = "backup_automation")

class BackupScheduler internal constructor(
    context: Context,
    private val runner: BackupRunner,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    val state: Flow<BackupState> = appContext.backupDataStore.data.map { it.toBackupState() }

    suspend fun enable(
        treeUri: Uri,
        intervalHours: Long = 24,
        retentionCount: Int = 7,
    ) {
        appContext.contentResolver.persistBackupTreePermission(treeUri)
        val interval = intervalHours.coerceAtLeast(1)
        val retention = retentionCount.coerceIn(1, 100)
        appContext.backupDataStore.edit {
            it[Keys.Enabled] = true
            it[Keys.TreeUri] = treeUri.toString()
            it[Keys.IntervalHours] = interval
            it[Keys.RetentionCount] = retention
        }
        schedule(interval)
    }

    suspend fun disable() {
        workManager.cancelUniqueWork(UniqueWorkName)
        appContext.backupDataStore.edit { it[Keys.Enabled] = false }
    }

    internal suspend fun runScheduledBackup(): BackupRunSummary {
        val current = state.first()
        if (!current.enabled || current.treeUri.isNullOrBlank()) {
            return BackupRunSummary(BackupResult.Disabled)
        }
        val timestamp = System.currentTimeMillis()
        return runCatching {
            runner.run(current.treeUri, current.retentionCount)
        }.fold(
            onSuccess = { summary ->
                record(timestamp, summary)
                summary
            },
            onFailure = { error ->
                val summary = BackupRunSummary(
                    result = BackupResult.Failed,
                    message = error.message ?: error::class.java.simpleName,
                )
                record(timestamp, summary)
                summary
            },
        )
    }

    private fun schedule(intervalHours: Long) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalHours,
            TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build(),
        ).build()
        workManager.enqueueUniquePeriodicWork(
            UniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private suspend fun record(timestamp: Long, summary: BackupRunSummary) {
        appContext.backupDataStore.edit {
            it[Keys.LastRun] = timestamp
            it[Keys.LastResult] = summary.result.name
            if (summary.message == null) it.remove(Keys.LastMessage)
            else it[Keys.LastMessage] = summary.message
        }
    }

    private object Keys {
        val Enabled = booleanPreferencesKey("enabled")
        val TreeUri = stringPreferencesKey("tree_uri")
        val IntervalHours = longPreferencesKey("interval_hours")
        val RetentionCount = intPreferencesKey("retention_count")
        val LastRun = longPreferencesKey("last_run_utc_millis")
        val LastResult = stringPreferencesKey("last_result")
        val LastMessage = stringPreferencesKey("last_message")
    }

    companion object {
        const val UniqueWorkName = "automatic-workout-backup"
    }

    private fun Preferences.toBackupState() = BackupState(
        enabled = this[Keys.Enabled] ?: false,
        treeUri = this[Keys.TreeUri],
        intervalHours = this[Keys.IntervalHours] ?: 24,
        retentionCount = this[Keys.RetentionCount] ?: 7,
        lastRunUtcMillis = this[Keys.LastRun],
        lastResult = this[Keys.LastResult]
            ?.let { runCatching { BackupResult.valueOf(it) }.getOrNull() }
            ?: BackupResult.NeverRun,
        lastMessage = this[Keys.LastMessage],
    )
}
