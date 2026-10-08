package com.lukr99.workout.settings

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val units: UnitSystem = UnitSystem.Metric,
    val defaultRestSeconds: Int = 120,
    /** Ember's own "reduce motion", on top of Android's "remove animations". */
    val reduceMotion: Boolean = false,
)
