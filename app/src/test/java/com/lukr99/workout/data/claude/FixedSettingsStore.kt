package com.lukr99.workout.data.claude

import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** [SettingsStore] in memory, for tests. */
class FixedSettingsStore(units: UnitSystem = UnitSystem.Metric) : SettingsStore {
    private val state = MutableStateFlow(AppSettings(units = units))
    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setUnits(units: UnitSystem) {
        state.value = state.value.copy(units = units)
    }

    override suspend fun setReduceMotion(on: Boolean) {
        state.value = state.value.copy(reduceMotion = on)
    }

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        state.value = state.value.copy(defaultRestSeconds = seconds)
    }

    override suspend fun replaceAll(settings: AppSettings) {
        state.value = settings
    }
}
