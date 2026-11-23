package com.lukr99.workout.settings

/**
 * The accent the owner picks in Settings: the colour of buttons, the current set ring, selected
 * chips and orange-style text. Ember orange is the default. The actual colours per theme live in
 * `ui/theme/EmberPalette`, so this stays free of UI code. Stored by name, so reordering is safe.
 */
enum class AccentColor { Ember, Blue, Green, Rose, Purple }
