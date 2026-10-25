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

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** The AndroidX DataStore [SettingsStore]: one `Preferences` file named "settings". */
class DataStoreSettingsStore(private val context: Context) : SettingsStore {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs -> prefs.toSettings() }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit {
            it[Keys.ThemeMode] = mode.name
        }
    }

    override suspend fun setUnits(units: UnitSystem) {
        context.settingsDataStore.edit {
            it[Keys.Units] = units.name
        }
    }

    override suspend fun setReduceMotion(on: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.ReduceMotion] = on
        }
    }

    override suspend fun setDefaultRestSeconds(seconds: Int) {
        context.settingsDataStore.edit {
            it[Keys.RestSeconds] = seconds.coerceIn(0, 3_600)
        }
    }

    override suspend fun replaceAll(settings: AppSettings) {
        context.settingsDataStore.edit {
            it[Keys.ThemeMode] = settings.themeMode.name
            it[Keys.Units] = settings.units.name
            it[Keys.RestSeconds] = settings.defaultRestSeconds.coerceIn(0, 3_600)
            it[Keys.ReduceMotion] = settings.reduceMotion
        }
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
