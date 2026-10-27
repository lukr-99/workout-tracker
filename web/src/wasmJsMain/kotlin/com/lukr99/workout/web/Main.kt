package com.lukr99.workout.web

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.ui.App
import com.lukr99.workout.ui.components.LocalReduceMotion
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.WorkoutTheme
import com.lukr99.workout.web.storage.WebStorage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * The web edition's entry point. Before the first frame it fetches the fonts and reads the stored
 * data, so the app never starts on an empty store that would then seed over the real one. Then it
 * shows the shared [App] on the page canvas, with the [StorageBanner] on top.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val scope = MainScope()
    val storage = WebStorage(scope)
    scope.launch {
        preloadWebResources()
        storage.database.open()
        // The screens read photos from memory while drawing, so they are loaded before the first frame.
        storage.photos.load()
        WebPhotos.archive = storage.photos
        storage.start()
        // Like WorkoutApp on Android: the starter catalog goes in once, into an empty store.
        storage.repository.ensureSeeded()
        val container = AppContainer(storage)
        CanvasBasedWindow(canvasElementId = "ember", title = "Ember") {
            // The same theme and motion setup as Android's MainActivity.
            val settings by container.settings.settings.collectAsState(initial = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }
            WorkoutTheme(dark = dark) {
                CompositionLocalProvider(LocalReduceMotion provides settings.reduceMotion) {
                    Box(Modifier.fillMaxSize().background(EmberTheme.colors.background).padding(rememberSafeAreaPadding())) {
                        App(container)
                        Column(Modifier.align(Alignment.TopCenter)) {
                            StorageBanner(storage)
                            InstallNote()
                        }
                    }
                }
            }
        }
    }
}
