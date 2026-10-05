package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The two colour sets from the approved redesign prototype: warm near-black for dark, warm off-white
 * for light, one ember orange in both. Text colours keep at least 4.5:1 contrast on their surface.
 */
object EmberPalette {
    private val Orange = Color(0xFFF97316)
    private val OnOrange = Color(0xFF1C0B00)

    val Dark = EmberColors(
        background = Color(0xFF0F0D0C),
        surface = Color(0xFF1A1715),
        surfaceRaised = Color(0xFF25211E),
        border = Color(0xFF342E2A),
        textPrimary = Color(0xFFF6F2ED),
        textSecondary = Color(0xFFBDB3A9),
        textTertiary = Color(0xFF9A9087),
        primary = Orange,
        primaryText = Color(0xFFFF8A3D),
        onPrimary = OnOrange,
        primarySoft = Orange.copy(alpha = 0.14f),
        success = Color(0xFF3DD68C),
        successSoft = Color(0xFF3DD68C).copy(alpha = 0.16f),
        warning = Color(0xFFF5B841),
        danger = Color(0xFFFF7A7A),
        onDanger = OnOrange,
        violet = Color(0xFFB79CFF),
        teal = Color(0xFF3FD5C0),
        focus = Color(0xFFFF8A3D),
        isDark = true,
    )

    val Light = EmberColors(
        background = Color(0xFFF7F4F0),
        surface = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFF1ECE6),
        border = Color(0xFFE2D9CF),
        textPrimary = Color(0xFF1B1714),
        textSecondary = Color(0xFF584F47),
        textTertiary = Color(0xFF6B625A),
        primary = Orange,
        primaryText = Color(0xFFB04A05),
        onPrimary = OnOrange,
        primarySoft = Orange.copy(alpha = 0.13f),
        success = Color(0xFF11784A),
        successSoft = Color(0xFF11784A).copy(alpha = 0.12f),
        warning = Color(0xFF8A5A00),
        danger = Color(0xFFC22F2F),
        onDanger = Color.White,
        violet = Color(0xFF6A4ED0),
        teal = Color(0xFF0E776B),
        focus = Color(0xFFB04A05),
        isDark = false,
    )
}
