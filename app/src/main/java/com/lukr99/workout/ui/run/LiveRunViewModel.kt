package com.lukr99.workout.ui.run

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.lukr99.workout.WorkoutApp
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.health.HealthConnectService
import com.lukr99.workout.data.location.LocationService
import com.lukr99.workout.data.location.RunSessionController
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val healthConnect: HealthConnectService,
    private val repo: RunRepository,
) : AndroidViewModel(application) {

    val state: StateFlow<LiveRunState> = controller.state
    val trace: StateFlow<List<TracePoint>> = controller.trace

    private val _plannedRoute = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    /** Faint reference line when a run was started from a saved route (empty otherwise). */
    val plannedRoute: StateFlow<List<Pair<Double, Double>>> = _plannedRoute.asStateFlow()

    /**
     * Arm the next run with a saved route (or clear it). The route is only ever a **reference** — the
     * run records its own trace and is never forced onto the route. Call before showing the run screen.
     */
    fun prepareRoute(routeId: String?) {
        if (routeId == null) {
            _plannedRoute.value = emptyList()
            controller.armRoute(null)
            return
        }
        viewModelScope.launch {
            val route = repo.getRoute(routeId)
            _plannedRoute.value = route?.let { r ->
                r.points.takeIf { it.isNotEmpty() }?.map { it.lat to it.lon }
                    ?: Polyline.decode(r.encodedPolyline)
            } ?: emptyList()
            controller.armRoute(routeId)
        }
    }

    /** Begin recording — spins up the foreground service, which promotes + starts the tracker. */
    fun start() {
        if (!controller.isRunning) LocationService.start(getApplication())
    }

    fun pause() = controller.pause()
    fun resume() = controller.resume()

    /** Finish + persist; the service tears itself down when it sees the finished state. */
    suspend fun finish(): Run {
        val run = controller.finish()
        // Best-effort mirror to Health Connect (idempotent; no-op without permission). Never blocks
        // or fails the save — the run is already persisted locally by the controller.
        runCatching { healthConnect.exportRuns(listOf(run)) }
        return run
    }

    /** Abandon without saving; the controller's idle state makes the service tear itself down. */
    fun discard() = controller.discard()

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                return LiveRunViewModel(
                    app,
                    app.container.runSessionController,
                    app.container.healthConnect,
                    app.container.runRepository,
                ) as T
            }
        }
    }
}
