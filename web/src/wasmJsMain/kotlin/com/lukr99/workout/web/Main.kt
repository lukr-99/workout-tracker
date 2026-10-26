package com.lukr99.workout.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.ui.App

/** The web edition's entry point: builds the app graph and shows the shared [App] on the page canvas. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ember", title = "Ember") {
        App(AppContainer())
    }
}
