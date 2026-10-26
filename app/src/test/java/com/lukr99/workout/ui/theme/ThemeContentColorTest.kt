package com.lukr99.workout.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Text without its own colour is drawn in the theme's text colour, not Compose's default black. */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class ThemeContentColorTest {
    @get:Rule
    val compose = createComposeRule()

    private fun contentColorIn(dark: Boolean): Color {
        var seen = Color.Unspecified
        compose.setContent { WorkoutTheme(dark = dark) { seen = LocalContentColor.current } }
        compose.waitForIdle()
        return seen
    }

    @Test
    fun darkThemeTextIsTheLightTextColour() {
        assertEquals(EmberPalette.Dark.textPrimary, contentColorIn(dark = true))
    }

    @Test
    fun lightThemeTextIsTheDarkTextColour() {
        assertEquals(EmberPalette.Light.textPrimary, contentColorIn(dark = false))
    }
}
