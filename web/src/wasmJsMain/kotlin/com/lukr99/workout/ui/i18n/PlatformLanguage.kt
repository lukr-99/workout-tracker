package com.lukr99.workout.ui.i18n

import kotlinx.browser.window

/*
 * Each platform supplies this file. The web edition reads the browser's own language, and has
 * nothing to apply: its date and number text read AppLanguage directly. Android's is under app/.
 */

/** The language the browser is set to. */
fun systemLanguage(): Language = Language.fromTag(window.navigator.language)

/** Nothing to do on the web. */
internal fun applyPlatformLanguage(language: Language) = Unit
