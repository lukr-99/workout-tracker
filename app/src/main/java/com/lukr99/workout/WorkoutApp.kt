package com.lukr99.workout

import android.app.Application
import androidx.work.Configuration
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.backup.BackupWorkerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-level owner of the [AppContainer], the composition root. Seeds the catalog once on first
 * run (empty DB), off the main thread, mirroring the MAUI `InitializeAsync`.
 *
 * It also configures WorkManager, so workers get their dependencies from a factory instead of
 * looking the container up; the manifest turns off WorkManager's default initializer for this.
 */
class WorkoutApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(BackupWorkerFactory { container.backup.runScheduledBackup() })
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.repository.ensureSeeded() }
        // Salvage a run whose process was killed mid-recording (crash buffer → saved run).
        appScope.launch { container.runSessionController.recoverIfNeeded() }
    }
}
