package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Named design tokens from docs/rework/02-design-system.md.
 *
 * The palette is minimal-dark, Lyfta-inspired: near-black surfaces, one ember-orange accent, and
 * a set of quiet per-metric / per-body-part hues that never fight the primary action color.
 * These raw tokens are mapped into Material 3 [androidx.compose.material3.ColorScheme]s in Theme.kt.
 */

// ── Dark (the product) ──────────────────────────────────────────────────────
val Bg = Color(0xFF0B0D10)          // app background (true near-black)
val Surface = Color(0xFF14181D)     // sections, sheets, nav bar
val SurfaceHigh = Color(0xFF1B2027) // raised rows, active set, menus
val Outline = Color(0xFF232A32)     // hairline dividers, input borders
val TextHi = Color(0xFFF2F5F8)      // primary text, numbers
val TextMid = Color(0xFF9AA4B0)     // labels, secondary text
val TextLo = Color(0xFF5C6672)      // hints, disabled, timestamps

// ── Accent (used sparingly) ─────────────────────────────────────────────────
val Ember = Color(0xFFF97316)       // primary action, current focus, FAB
val EmberPress = Color(0xFFFB8B3D)  // pressed/hover tint of primary
val OnEmber = Color(0xFF160B03)     // text/icon on the accent

// ── Semantic ────────────────────────────────────────────────────────────────
val Positive = Color(0xFF34D399)    // PRs, gains, success
val Warning = Color(0xFFFBBF24)     // RPE-high, caution
val Danger = Color(0xFFF26D6D)      // destructive confirm (discard/delete)

// ── Light theme ─────────────────────────────────────────────────────────────
val LightBg = Color(0xFFF4F6F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFEDF0F5)
val LightOutline = Color(0xFFD8DEE6)
val LightTextHi = Color(0xFF0C1220)
val LightTextMid = Color(0xFF556070)
val LightTextLo = Color(0xFF8A93A0)
val EmberDeep = Color(0xFFE4670C)   // deeper ember for light-mode contrast

/**
 * Per-metric / per-body-part accents (kept distinct from the ember primary), for charts and tags.
 * See 02-design-system.md.
 */
object Accents {
    val StrengthVolume = Ember
    val Cardio = Color(0xFF22D3EE)  // teal
    val E1rm = Color(0xFFA78BFA)    // violet
    val Reps = Color(0xFF34D399)    // green

    val Chest = Color(0xFFF472B6)
    val Back = Color(0xFF60A5FA)
    val Legs = Color(0xFF34D399)
    val Shoulders = Color(0xFFFBBF24)
    val Arms = Color(0xFFA78BFA)
    val Core = Color(0xFF22D3EE)
}
