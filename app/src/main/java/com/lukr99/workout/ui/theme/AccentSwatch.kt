package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colours one accent contributes to a theme. [fill] is the button and ring colour, [onFill] the
 * text on it, [textOnDark] and [textOnLight] the accent as text or an icon on that theme's surfaces.
 * Every pair keeps at least 4.5:1 contrast; `AccentPaletteTest` checks it for both themes.
 */
internal data class AccentSwatch(
    val fill: Color,
    val onFill: Color,
    val textOnDark: Color,
    val textOnLight: Color,
)
