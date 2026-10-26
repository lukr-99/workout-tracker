package com.lukr99.workout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.lukr99.workout.data.music.SpotifyController
import com.lukr99.workout.data.music.StubSpotifyController

/**
 * The music controller in scope for the live screens. Provided once at the app root. Android only:
 * the web edition has no controller and opens the music app by link.
 */
val LocalSpotify: ProvidableCompositionLocal<SpotifyController> =
    staticCompositionLocalOf { StubSpotifyController }

/** Convenience for the app root to inject the controller into the tree. */
@Composable
fun ProvideSpotify(controller: SpotifyController, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpotify provides controller, content = content)
}
