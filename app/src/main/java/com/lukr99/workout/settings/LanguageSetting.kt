package com.lukr99.workout.settings

/**
 * The language choice in Settings. [System] follows the phone or browser and falls back to English
 * when Ember has no translation for it. Kept in [DevicePrefs]: a language belongs to this device,
 * not to the owner's data, so it stays out of backups.
 */
enum class LanguageSetting { System, English, Czech }
