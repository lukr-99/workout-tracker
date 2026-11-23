package com.lukr99.workout.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.lukr99.workout.settings.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every accent keeps readable text in both themes, and Ember is exactly the original palette. */
class AccentPaletteTest {

    private fun contrast(a: Color, b: Color): Double {
        val hi = maxOf(a.luminance(), b.luminance()).toDouble()
        val lo = minOf(a.luminance(), b.luminance()).toDouble()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertReadable(what: String, a: Color, b: Color) =
        assertTrue("$what has contrast ${contrast(a, b)}, below 4.5", contrast(a, b) >= 4.5)

    @Test
    fun emberAccentIsTheOriginalPalette() {
        assertEquals(EmberPalette.Dark, EmberPalette.colors(dark = true, accent = AccentColor.Ember))
        assertEquals(EmberPalette.Light, EmberPalette.colors(dark = false, accent = AccentColor.Ember))
    }

    @Test
    fun accentTextIsReadableOnEverySurfaceInBothThemes() {
        for (accent in AccentColor.entries) {
            for (dark in listOf(true, false)) {
                val c = EmberPalette.colors(dark, accent)
                val where = "$accent ${if (dark) "dark" else "light"}"
                assertReadable("$where text on background", c.primaryText, c.background)
                assertReadable("$where text on surface", c.primaryText, c.surface)
                assertReadable("$where text on raised surface", c.primaryText, c.surfaceRaised)
            }
        }
    }

    @Test
    fun textOnTheAccentFillIsReadableInBothThemes() {
        for (accent in AccentColor.entries) {
            for (dark in listOf(true, false)) {
                val c = EmberPalette.colors(dark, accent)
                assertReadable("$accent onPrimary on primary", c.onPrimary, c.primary)
            }
            // Light mode gives Material white text on the darker accent text colour.
            assertReadable("$accent white on light primaryText", Color.White, EmberPalette.colors(false, accent).primaryText)
        }
    }

    @Test
    fun accentsChangeOnlyTheAccentRoles() {
        val base = EmberPalette.colors(dark = true, accent = AccentColor.Ember)
        for (accent in AccentColor.entries - AccentColor.Ember) {
            val c = EmberPalette.colors(dark = true, accent = accent)
            assertNotEquals(base.primary, c.primary)
            assertEquals(base.background, c.background)
            assertEquals(base.surface, c.surface)
            assertEquals(base.textPrimary, c.textPrimary)
            assertEquals(base.danger, c.danger)
            assertTrue(c.isDark)
        }
    }

    @Test
    fun everyAccentHasItsOwnFill() {
        val fills = AccentColor.entries.map { AccentPalette.swatch(it).fill }
        assertEquals(fills.size, fills.toSet().size)
    }
}
