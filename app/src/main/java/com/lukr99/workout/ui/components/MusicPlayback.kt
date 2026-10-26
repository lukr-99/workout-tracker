package com.lukr99.workout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

/**
 * The connected music player while the calling screen is shown, or null when none is connected
 * (the shipping case: only the open button shows).
 *
 * Each platform supplies this file; the web edition's is under web/.
 */
@Composable
fun rememberMusicTransport(): MusicTransport? {
    val controller = LocalSpotify.current
    val context = LocalContext.current
    val available by controller.available.collectAsState()
    val track by controller.track.collectAsState()

    DisposableEffect(controller) {
        controller.connect(context)
        onDispose { controller.disconnect() }
    }

    val current = track
    return if (available && current != null) {
        MusicTransport(current.isPlaying, controller::playPause, controller::next, controller::previous)
    } else {
        null
    }
}

/** A function that brings the music app to the front (Android: Spotify, with store fallbacks). */
@Composable
fun rememberOpenMusicApp(): () -> Unit {
    val controller = LocalSpotify.current
    val context = LocalContext.current
    return { controller.openSpotify(context) }
}
