package com.lukr99.workout

import android.app.Application
import com.lukr99.workout.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-level owner of the [AppContainer]. Seeds the catalog once on first run (empty DB), off
 * the main thread, mirroring the MAUI `InitializeAsync`.
 */
class WorkoutApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.repository.ensureSeeded() }
        // Salvage a run whose process was killed mid-recording (crash buffer → saved run).
        appScope.launch { container.runSessionController.recoverIfNeeded() }
    }
}
