package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colours for the lines of a chart with several series, taken from the theme tokens so both themes
 * stay readable. Each line is also named in the legend, so colour is never the only signal. The
 * unknown-place line is always the muted grey.
 */
object SeriesColors {
    fun of(colors: EmberColors, index: Int, isUnknown: Boolean = false): Color {
        if (isUnknown) return colors.textTertiary
        val palette = listOf(colors.violet, colors.primaryText, colors.teal, colors.warning, colors.success)
        return palette[index % palette.size]
    }
}
