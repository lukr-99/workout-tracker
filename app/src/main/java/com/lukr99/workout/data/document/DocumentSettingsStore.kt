package com.lukr99.workout.data.document

import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The [SettingsStore] over a [DocumentDatabase]. Settings live in the document's
 * `data.settings`, in the same [SettingsSnapshot] shape a backup uses, so a restore or an export
 * needs no mapping of its own. A missing snapshot or an unknown name reads as the default.
 */
class DocumentSettingsStore(private val database: DocumentDatabase) : SettingsStore {

    override val settings: Flow<AppSettings> = database.document
        .map { it.data.settings.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) = change { it.copy(themeMode = mode.name) }

    override suspend fun setUnits(units: UnitSystem) = change { it.copy(units = units.name) }

    override suspend fun setReduceMotion(on: Boolean) = change { it.copy(reduceMotion = on) }

    override suspend fun setDefaultRestSeconds(seconds: Int) = change { it.copy(defaultRestSeconds = seconds.coerceIn(0, 3_600)) }

    override suspend fun replaceAll(settings: AppSettings) = change {
        SettingsSnapshot(
            themeMode = settings.themeMode.name,
            units = settings.units.name,
            defaultRestSeconds = settings.defaultRestSeconds.coerceIn(0, 3_600),
            reduceMotion = settings.reduceMotion,
        )
    }

    private suspend fun change(edit: (SettingsSnapshot) -> SettingsSnapshot) = database.update { document ->
        document.edit { copy(settings = edit(settings ?: SettingsSnapshot())) }
    }

    private fun SettingsSnapshot?.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        if (this == null) return defaults
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: defaults.themeMode,
            units = UnitSystem.entries.firstOrNull { it.name == units } ?: defaults.units,
            defaultRestSeconds = defaultRestSeconds,
            reduceMotion = reduceMotion,
        )
    }
}
