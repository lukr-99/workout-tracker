package com.lukr99.workout.web.platform

import com.lukr99.workout.settings.AccentColor
import com.lukr99.workout.settings.ThemeMode

/**
 * Keeps a copy of the theme mode and accent in the browser's local storage, so `index.html` can
 * paint the loading screen in the right colours before the app has opened its own storage. The
 * app's own settings stay the source of truth; this is only a hint for the first paint.
 */
internal fun rememberAppearance(mode: ThemeMode, accent: AccentColor) {
    saveAppearance("${mode.name}|${accent.name}")
}

private fun saveAppearance(value: String): Unit = js(
    """(function () {
        try { localStorage.setItem('ember.appearance', value); } catch (e) {}
        if (window.emberApplyAppearance) window.emberApplyAppearance();
    })()""",
)
