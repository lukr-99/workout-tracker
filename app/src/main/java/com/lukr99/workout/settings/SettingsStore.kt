package com.lukr99.workout.settings

import kotlinx.coroutines.flow.Flow

/**
 * User settings: theme mode, unit system (kg/lb), rest-timer default.
 *
 * The typed flow is read by the Settings screen and the theme root (see MainActivity). Writes are
 * suspending edits; the UI collects the flow. [DataStoreSettingsStore] is the Android implementation.
 */
interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUnits(units: UnitSystem)

    suspend fun setReduceMotion(on: Boolean)

    suspend fun setDefaultRestSeconds(seconds: Int)

    /** Writes every setting at once, for restoring a backup or resetting to defaults. */
    suspend fun replaceAll(settings: AppSettings)
}
