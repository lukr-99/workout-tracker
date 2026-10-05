package com.lukr99.workout.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The semantic colours every component reads, named after CodePrint's theme contract. A light and a
 * dark set live in [EmberPalette]; components never pick a raw colour, so both themes always work.
 */
@Immutable
data class EmberColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Fills: buttons, the Start button, the current set ring. The same orange in both themes. */
    val primary: Color,
    /** Orange text and icons on a surface. Darker in light mode so it stays readable. */
    val primaryText: Color,
    val onPrimary: Color,
    /** A faint orange wash behind selected or in-progress things. */
    val primarySoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val danger: Color,
    val onDanger: Color,
    /** Estimated 1RM and other strength trends. */
    val violet: Color,
    /** Cardio and runs. */
    val teal: Color,
    val focus: Color,
    val isDark: Boolean,
)
