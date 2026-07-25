package com.lukr99.workout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material 3 schemes built from the design-system tokens (Color.kt).
 *
 * Dark is the product; light is supported. The tokens map onto the M3 roles we actually use:
 * `primary` = ember, `background`/`surface` = near-black tiers, `surfaceVariant` = raised rows,
 * `onSurfaceVariant` = mid text (labels), `outline` = hairlines.
 */
private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = OnEmber,
    primaryContainer = SurfaceHigh,
    onPrimaryContainer = TextHi,
    secondary = Accents.E1rm,
    onSecondary = OnEmber,
    tertiary = Accents.Cardio,
    background = Bg,
    onBackground = TextHi,
    surface = Surface,
    onSurface = TextHi,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextMid,
    outline = Outline,
    outlineVariant = Outline,
    error = Danger,
    onError = OnEmber,
)

private val LightColors = lightColorScheme(
    primary = EmberDeep,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = LightSurfaceHigh,
    onPrimaryContainer = LightTextHi,
    secondary = Accents.E1rm,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Accents.Cardio,
    background = LightBg,
    onBackground = LightTextHi,
    surface = LightSurface,
    onSurface = LightTextHi,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextMid,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = Danger,
    onError = androidx.compose.ui.graphics.Color.White,
)

/**
 * App theme. Dark-first: defaults to the system setting, but every screen is designed dark.
 * A future in-app override (DataStore) will pass an explicit [dark] value.
 */
@Composable
fun WorkoutTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = WorkoutTypography,
        content = content,
    )
}
