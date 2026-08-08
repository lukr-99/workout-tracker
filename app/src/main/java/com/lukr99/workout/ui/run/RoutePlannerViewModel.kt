package com.lukr99.workout.ui.run

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.lukr99.workout.WorkoutApp
import com.lukr99.workout.data.routing.LatLon
import com.lukr99.workout.data.routing.RoutingClient
import com.lukr99.workout.data.routing.SnappedRoute
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Route planner (R3): accumulates tapped waypoints, snaps them to roads/paths via [RoutingClient]
 * after each edit, exposes the snapped line + distance for the map/UI, and saves the result as a
 * [Route] through [RunRepository]. The snapped polyline (not the raw taps) is what gets saved.
 */
class RoutePlannerViewModel(
    application: Application,
    private val routing: RoutingClient,
    private val repo: RunRepository,
) : AndroidViewModel(application) {

    private val _waypoints = MutableStateFlow<List<LatLon>>(emptyList())
    val waypoints: StateFlow<List<LatLon>> = _waypoints.asStateFlow()

    private val _snapped = MutableStateFlow<SnappedRoute?>(null)
    val snapped: StateFlow<SnappedRoute?> = _snapped.asStateFlow()

    private val _snapping = MutableStateFlow(false)
    val snapping: StateFlow<Boolean> = _snapping.asStateFlow()

    private var snapJob: Job? = null

    fun addWaypoint(lat: Double, lon: Double) {
        _waypoints.value = _waypoints.value + LatLon(lat, lon)
        resnap()
    }

    fun undo() {
        if (_waypoints.value.isEmpty()) return
        _waypoints.value = _waypoints.value.dropLast(1)
        resnap()
    }

    fun clear() {
        _waypoints.value = emptyList()
        _snapped.value = null
        snapJob?.cancel()
        _snapping.value = false
    }

    private fun resnap() {
        val wps = _waypoints.value
        snapJob?.cancel()
        if (wps.size < 2) {
            _snapped.value = null
            _snapping.value = false
            return
        }
        snapJob = viewModelScope.launch {
            _snapping.value = true
            _snapped.value = routing.snap(wps)
            _snapping.value = false
        }
    }

    /** Persist the current snapped route under [name]; returns it, or null if nothing is snapped. */
    suspend fun save(name: String): Route? {
        val snap = _snapped.value ?: return null
        if (snap.points.size < 2) return null
        val route = Route(
            name = name.trim().ifBlank { "Route" },
            distanceMeters = snap.distanceMeters,
            elevationGainM = 0.0,
            encodedPolyline = Polyline.encodeRoute(snap.points),
            createdAtUtc = System.currentTimeMillis(),
            points = snap.points,
        )
        repo.saveRoute(route)
        return route
    }

    /** Distance of the snapped route (0 until at least two waypoints snap). */
    val distanceMeters: Double get() = _snapped.value?.distanceMeters ?: 0.0

    fun snappedLatLon(): List<Pair<Double, Double>> =
        _snapped.value?.points?.map { it.lat to it.lon } ?: emptyList()

    fun waypointLatLon(): List<Pair<Double, Double>> = _waypoints.value.map { it.lat to it.lon }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                return RoutePlannerViewModel(app, app.container.routingClient, app.container.runRepository) as T
            }
        }
    }
}
