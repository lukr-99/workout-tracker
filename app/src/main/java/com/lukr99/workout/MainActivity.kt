package com.lukr99.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.lukr99.workout.data.claude.ClaudeNotifications
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.LanguageSetting
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.ui.App
import com.lukr99.workout.ui.components.LocalReduceMotion
import com.lukr99.workout.ui.i18n.ProvideLanguage
import com.lukr99.workout.ui.theme.WorkoutTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Single-activity Compose host. Edge-to-edge; the theme is [WorkoutTheme] (dark-first) with an
 * in-app override read live from the [com.lukr99.workout.settings.SettingsStore]. Share / open-document
 * intents land here in later phases.
 */
class MainActivity : ComponentActivity() {

    /**
     * Claude's share requests this phone missed while Ember was closed or without push: answered by
     * the share mode, or shown as the Allow and Deny notification (docs/claude-connector.md).
     */
    override fun onResume() {
        super.onResume()
        val claude = (application as WorkoutApp).container.claude
        lifecycleScope.launch {
            val waiting = runCatching { claude.sharing.catchUp() }.getOrDefault(emptyList())
            waiting.forEach { ClaudeNotifications.showShareRequest(this@MainActivity, it.id) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WorkoutApp).container
        // Read the saved theme before the first frame, with the splash screen still up, so a phone
        // set to Dark never draws one light frame (and an accent never starts as orange).
        val firstSettings = runBlocking {
            withTimeoutOrNull(FIRST_SETTINGS_WAIT_MS) { container.settings.settings.first() }
        } ?: AppSettings()
        val firstLanguage = runBlocking {
            withTimeoutOrNull(FIRST_SETTINGS_WAIT_MS) { container.devicePrefs.language.first() }
        } ?: LanguageSetting.System
        setContent {
            val language by container.devicePrefs.language.collectAsState(initial = firstLanguage)
            val settings by container.settings.settings.collectAsState(initial = firstSettings)
            val dark = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }
            // Keep system-bar icons legible against whichever scheme is active.
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
            ProvideLanguage(language) {
                WorkoutTheme(dark = dark, accent = settings.accent) {
                    CompositionLocalProvider(LocalReduceMotion provides settings.reduceMotion) {
                        App(container)
                    }
                }
            }
        }
    }

    private companion object {
        /** Longest the first frame waits for the saved settings. DataStore normally answers in a few ms. */
        const val FIRST_SETTINGS_WAIT_MS = 400L
    }
}
