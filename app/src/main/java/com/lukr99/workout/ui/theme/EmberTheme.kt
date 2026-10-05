package com.lukr99.workout.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalEmberColors = staticCompositionLocalOf { EmberPalette.Dark }

/** Reads the current theme's semantic colours, like `MaterialTheme.colorScheme`. */
object EmberTheme {
    val colors: EmberColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEmberColors.current
}
