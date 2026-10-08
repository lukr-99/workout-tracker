package com.lukr99.workout.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed user settings: theme mode, unit system (kg/lb), rest-timer default.
 *
 * The typed flows are read by the Settings screen and the theme root (see MainActivity). Writes are
 * suspending edits; the UI collects the flows. Kept intentionally small — one `Preferences` file.
 */
val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun setThemeMode(mode: ThemeMode) = context.settingsDataStore.edit {
        it[Keys.ThemeMode] = mode.name
    }

    suspend fun setUnits(units: UnitSystem) = context.settingsDataStore.edit {
        it[Keys.Units] = units.name
    }

    suspend fun setReduceMotion(on: Boolean) = context.settingsDataStore.edit {
        it[Keys.ReduceMotion] = on
    }

    suspend fun setDefaultRestSeconds(seconds: Int) = context.settingsDataStore.edit {
        it[Keys.RestSeconds] = seconds.coerceIn(0, 3_600)
    }

    /** Writes every setting at once, for restoring a backup or resetting to defaults. */
    suspend fun replaceAll(settings: AppSettings) = context.settingsDataStore.edit {
        it[Keys.ThemeMode] = settings.themeMode.name
        it[Keys.Units] = settings.units.name
        it[Keys.RestSeconds] = settings.defaultRestSeconds.coerceIn(0, 3_600)
        it[Keys.ReduceMotion] = settings.reduceMotion
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        themeMode = this[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System,
        units = this[Keys.Units]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
            ?: UnitSystem.Metric,
        defaultRestSeconds = this[Keys.RestSeconds] ?: 120,
        reduceMotion = this[Keys.ReduceMotion] ?: false,
    )

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val Units = stringPreferencesKey("units")
        val RestSeconds = intPreferencesKey("default_rest_seconds")
        val ReduceMotion = booleanPreferencesKey("reduce_motion")
    }
}
