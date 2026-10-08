package com.lukr99.workout.data.transfer

import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.flow.first

/** [SettingsArchive] over the DataStore-backed [SettingsStore]. Unknown names fall back to defaults. */
class DataStoreSettingsArchive(private val store: SettingsStore) : SettingsArchive {

    override suspend fun snapshot(): SettingsSnapshot {
        val current = store.settings.first()
        return SettingsSnapshot(
            themeMode = current.themeMode.name,
            units = current.units.name,
            defaultRestSeconds = current.defaultRestSeconds,
            reduceMotion = current.reduceMotion,
        )
    }

    override suspend fun restore(snapshot: SettingsSnapshot) {
        val defaults = AppSettings()
        store.replaceAll(
            AppSettings(
                themeMode = ThemeMode.entries.firstOrNull { it.name == snapshot.themeMode } ?: defaults.themeMode,
                units = UnitSystem.entries.firstOrNull { it.name == snapshot.units } ?: defaults.units,
                defaultRestSeconds = snapshot.defaultRestSeconds,
                reduceMotion = snapshot.reduceMotion,
            ),
        )
    }

    override suspend fun reset() {
        store.replaceAll(AppSettings())
    }
}
