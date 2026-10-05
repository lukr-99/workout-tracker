package com.lukr99.workout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App theme. [EmberTheme.colors] carries the semantic colours; the Material scheme is derived from
 * the same set, so Material components (sheets, dialogs, switches) match in both themes.
 */
@Composable
fun WorkoutTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) EmberPalette.Dark else EmberPalette.Light
    CompositionLocalProvider(LocalEmberColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = WorkoutTypography,
            content = content,
        )
    }
}

private fun EmberColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    // Material components and older screens use `primary` for orange text as well as fills. In
    // light mode the bright fill orange is too pale for text, so Material gets the darker text
    // orange there; components that want the bright fill read EmberTheme.colors.primary.
    return base.copy(
        primary = if (isDark) primary else primaryText,
        onPrimary = if (isDark) onPrimary else androidx.compose.ui.graphics.Color.White,
        primaryContainer = primarySoft,
        onPrimaryContainer = textPrimary,
        secondary = violet,
        onSecondary = onPrimary,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = textPrimary,
        tertiary = teal,
        onTertiary = onPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = textSecondary,
        surfaceTint = surface,
        surfaceBright = surfaceRaised,
        surfaceDim = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        inverseSurface = textPrimary,
        inverseOnSurface = background,
        inversePrimary = primaryText,
        outline = border,
        outlineVariant = border,
        error = danger,
        onError = onDanger,
        scrim = background,
    )
}
