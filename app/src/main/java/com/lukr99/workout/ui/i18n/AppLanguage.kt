package com.lukr99.workout.ui.i18n

import androidx.compose.runtime.compositionLocalOf

/** The language the screens are drawn in. The app root provides it from the saved setting. */
val LocalLanguage = compositionLocalOf { Language.English }

/**
 * The language non-composable code (date and number formatting, notifications) reads. The app root
 * sets it together with [LocalLanguage]; [applyLanguage] also tells the platform, so dates and
 * decimals follow.
 */
object AppLanguage {
    var current: Language = Language.English
        private set

    fun set(language: Language) {
        current = language
        applyPlatformLanguage(language)
    }
}
