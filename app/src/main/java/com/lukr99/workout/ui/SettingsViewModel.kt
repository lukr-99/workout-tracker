package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The preferences every platform has: theme, units, reduce motion and default rest. Health Connect,
 * automatic backup, the wger catalog and the updater belong to the Android-only
 * [com.lukr99.workout.ui.android.AndroidSettingsViewModel].
 */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }.let { }
    fun setUnits(units: UnitSystem) = viewModelScope.launch { store.setUnits(units) }.let { }
    fun setReduceMotion(on: Boolean) = viewModelScope.launch { store.setReduceMotion(on) }.let { }
    fun setDefaultRest(seconds: Int) = viewModelScope.launch {
        store.setDefaultRestSeconds(seconds)
    }.let { }

    companion object {
        fun factory(store: SettingsStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(store) }
            }
    }
}
