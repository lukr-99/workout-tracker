package com.lukr99.workout.data.export

import kotlinx.serialization.Serializable

/**
 * The owner's app settings inside a backup (export 1.8). Enum values travel by name so a renamed or
 * reordered enum cannot silently change meaning; an unknown name falls back to the default on
 * restore. Device-bound settings (the automatic backup folder and schedule) are left out on purpose.
 */
@Serializable
data class SettingsSnapshot(
    val themeMode: String = "System",
    val units: String = "Metric",
    val defaultRestSeconds: Int = 120,
)
