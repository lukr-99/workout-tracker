package com.lukr99.workout.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

/** Light/dark override on top of the system setting (dark is the product; see 02-design-system.md). */
enum class ThemeMode { System, Dark, Light }

/** Display units for weights/distances. Storage is always metric (kg/km); this is display-only. */
enum class UnitSystem { Metric, Imperial }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val units: UnitSystem = UnitSystem.Metric,
    val defaultRestSeconds: Int = 120,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun setThemeMode(mode: ThemeMode) = context.settingsDataStore.edit {
        it[Keys.ThemeMode] = mode.name
    }

    suspend fun setUnits(units: UnitSystem) = context.settingsDataStore.edit {
        it[Keys.Units] = units.name
    }

    suspend fun setDefaultRestSeconds(seconds: Int) = context.settingsDataStore.edit {
        it[Keys.RestSeconds] = seconds.coerceIn(0, 3_600)
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        themeMode = this[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System,
        units = this[Keys.Units]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
            ?: UnitSystem.Metric,
        defaultRestSeconds = this[Keys.RestSeconds] ?: 120,
    )

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val Units = stringPreferencesKey("units")
        val RestSeconds = intPreferencesKey("default_rest_seconds")
    }
}
