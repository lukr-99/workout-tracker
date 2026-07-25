package com.lukr99.workout.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore-backed user settings: theme mode, unit system (kg/lb), rest-timer defaults.
 *
 * Phase 0 stub: only the DataStore delegate is wired so the dependency is exercised. The typed
 * flows + setters (theme override, units) come with the Settings screen in a later phase.
 */
val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    // Phase 1+: expose Flow<ThemeMode>, Flow<UnitSystem>, restTimerSeconds, and their setters.
}
