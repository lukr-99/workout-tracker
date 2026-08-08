package com.lukr99.workout.ui.run

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.lukr99.workout.WorkoutApp
import com.lukr99.workout.data.location.LocationService
import com.lukr99.workout.data.location.RunSessionController
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlinx.coroutines.flow.StateFlow

/**
 * Live-run screen state + intents. A thin seam over [RunSessionController]: it re-exposes the
 * controller's [state]/[trace] flows to Compose and translates UI actions into controller calls,
 * starting/stopping the [LocationService] foreground service for GPS + the ongoing notification.
 *
 * Because the controller is a process singleton, the VM re-attaches to an in-progress run for free
 * when the screen is re-entered (e.g. reopened from the notification) — the flows already hold it.
 */
class LiveRunViewModel(
    application: Application,
    private val controller: RunSessionController,
) : AndroidViewModel(application) {

    val state: StateFlow<LiveRunState> = controller.state
    val trace: StateFlow<List<TracePoint>> = controller.trace

    /** Begin recording — spins up the foreground service, which promotes + starts the tracker. */
    fun start() {
        if (!controller.isRunning) LocationService.start(getApplication())
    }

    fun pause() = controller.pause()
    fun resume() = controller.resume()

    /** Finish + persist; the service tears itself down when it sees the finished state. */
    suspend fun finish(): Run = controller.finish()

    /** Abandon without saving; the controller's idle state makes the service tear itself down. */
    fun discard() = controller.discard()

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                return LiveRunViewModel(app, app.container.runSessionController) as T
            }
        }
    }
}
