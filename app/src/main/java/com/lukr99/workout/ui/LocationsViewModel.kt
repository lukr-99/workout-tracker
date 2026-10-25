package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.NoPlaceSuggester
import com.lukr99.workout.data.PlaceSuggester
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationKind
import com.lukr99.workout.settings.DevicePrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Training locations for every screen that shows or changes them: the Settings list, the location
 * picker, moving workouts in history, and Home's one-time setup card.
 */
class LocationsViewModel(
    private val repo: WorkoutRepository,
    private val prefs: DevicePrefs,
    private val suggester: PlaceSuggester = NoPlaceSuggester,
) : ViewModel() {

    /** Every location, archived ones included. */
    val locations: StateFlow<List<Location>> =
        repo.observeLocations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val setup: StateFlow<LocationSetup?> =
        combine(repo.observeLocations(), repo.observeHistory(), prefs.locationSetupDismissed) { locations, history, dismissed ->
            LocationSetup.due(locations, history.count { it.locationId == null }, dismissed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Adds a gym called [name] and hands it to [onAdded]. A blank name is ignored. */
    fun addGym(name: String, onAdded: (Location) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch { onAdded(repo.saveLocation(Location(name = name, kind = LocationKind.Gym))) }
    }

    fun rename(location: Location, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.saveLocation(location.copy(name = name)) }
    }

    fun setArchived(id: String, archived: Boolean) {
        viewModelScope.launch { repo.setLocationArchived(id, archived) }
    }

    /** Moves [sessionIds] to [locationId] (null is "Unknown location") and reports how many moved. */
    fun moveWorkouts(sessionIds: Collection<String>, locationId: String?, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.moveWorkoutsToLocation(sessionIds, locationId)) }
    }

    /**
     * Names the main gym from Home's setup card. With [assignPast], every workout without a location
     * moves there. Reports how many moved.
     */
    fun finishSetup(name: String, assignPast: Boolean, onDone: (Int) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val gym = repo.saveLocation(Location(name = name, kind = LocationKind.Gym))
            onDone(if (assignPast) repo.assignUnknownWorkoutsTo(gym.id) else 0)
        }
    }

    /** Whether the app already has location permission. Settings asks for it; nothing else does. */
    fun hasLocationPermission(): Boolean = suggester.hasPermission()

    /**
     * Saves where the phone is now as the position of [location] (a gym or Home). Reports whether a
     * fix came in time. The suggestion at workout start matches against these positions.
     */
    fun setPositionHere(location: Location, onDone: (Boolean) -> Unit) {
        if (location.kind == LocationKind.Outdoors) return onDone(false)
        viewModelScope.launch {
            val fix = suggester.currentFix()
            if (fix != null) {
                repo.saveLocation(location.copy(latitude = fix.latitude, longitude = fix.longitude))
                prefs.markPositionOfferAnswered(location.id)
            }
            onDone(fix != null)
        }
    }

    /** "Not now" on the setup card. Locations stay in Settings. */
    fun dismissSetup() {
        viewModelScope.launch { prefs.dismissLocationSetup() }
    }

    companion object {
        fun factory(repo: WorkoutRepository, prefs: DevicePrefs, suggester: PlaceSuggester): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { LocationsViewModel(repo, prefs, suggester) }
            }
    }
}
