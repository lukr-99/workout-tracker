package com.lukr99.workout.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.lukr99.workout.settings.LanguageSetting

/**
 * Gives every screen below it the chosen language. It also sets [AppLanguage] before the content
 * composes, so dates and decimals are right on the first frame and after a change in Settings.
 */
@Composable
fun ProvideLanguage(setting: LanguageSetting, content: @Composable () -> Unit) {
    val language = remember(setting) { Language.resolve(setting, systemLanguage()) }
    // Set during composition on purpose: formatting code reads it while this same pass draws.
    AppLanguage.set(language)
    CompositionLocalProvider(LocalLanguage provides language, content = content)
}
