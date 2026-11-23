package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color
import com.lukr99.workout.settings.AccentColor

internal object AccentPalette {

    fun swatch(accent: AccentColor): AccentSwatch = when (accent) {
        AccentColor.Ember -> AccentSwatch(Color(0xFFF97316), Color(0xFF1C0B00), Color(0xFFFF8A3D), Color(0xFFB04A05))
        AccentColor.Blue -> AccentSwatch(Color(0xFF3B82F6), Color(0xFF0B1220), Color(0xFF7DB2FF), Color(0xFF1D4ED8))
        AccentColor.Green -> AccentSwatch(Color(0xFF22C55E), Color(0xFF04170A), Color(0xFF4ADE80), Color(0xFF166534))
        AccentColor.Rose -> AccentSwatch(Color(0xFFF43F5E), Color(0xFF1F0309), Color(0xFFFF7A90), Color(0xFFBE123C))
        AccentColor.Purple -> AccentSwatch(Color(0xFF8B5CF6), Color(0xFF0E0720), Color(0xFFB79CFF), Color(0xFF6D28D9))
    }
}

/**
 * The colour set for a theme and an accent. Ember keeps the exact [EmberPalette] sets, so the
 * default look does not change; the other accents swap only the orange roles.
 */
fun EmberPalette.colors(dark: Boolean, accent: AccentColor): EmberColors {
    val base = if (dark) Dark else Light
    if (accent == AccentColor.Ember) return base
    val swatch = AccentPalette.swatch(accent)
    val text = if (dark) swatch.textOnDark else swatch.textOnLight
    return base.copy(
        primary = swatch.fill,
        primaryText = text,
        onPrimary = swatch.onFill,
        primarySoft = swatch.fill.copy(alpha = if (dark) 0.14f else 0.13f),
        focus = text,
    )
}
