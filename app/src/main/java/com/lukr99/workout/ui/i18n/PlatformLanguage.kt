package com.lukr99.workout.ui.i18n

import android.content.res.Resources
import java.util.Locale

/*
 * Each platform supplies this file. Android reads the phone's own language from the system
 * resources, which Locale.setDefault below never changes, and points the JVM default locale at the
 * chosen language so java.time and number formatting follow it. The web edition's is under web/.
 */

/** The language the phone itself is set to. */
fun systemLanguage(): Language = Language.fromTag(Resources.getSystem().configuration.locales[0].toLanguageTag())

/** Makes dates and decimals follow [language]. */
internal fun applyPlatformLanguage(language: Language) {
    Locale.setDefault(Locale.forLanguageTag(language.code))
}
