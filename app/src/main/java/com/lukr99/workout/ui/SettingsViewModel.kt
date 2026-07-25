package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Preferences: theme override, display units, and the default rest-timer duration. */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }.let { }
    fun setUnits(units: UnitSystem) = viewModelScope.launch { store.setUnits(units) }.let { }
    fun setDefaultRest(seconds: Int) = viewModelScope.launch { store.setDefaultRestSeconds(seconds) }.let { }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container.settings) as T
        }
    }
}
