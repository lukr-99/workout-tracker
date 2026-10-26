package com.lukr99.workout.ui.components

import androidx.compose.runtime.Composable
import com.lukr99.workout.web.platform.openSpotifyApp

/**
 * The connected music player, or null when none is connected. A web page cannot control Spotify, so
 * the web edition always answers null and only the open button shows.
 *
 * Each platform supplies this file.
 */
@Composable
fun rememberMusicTransport(): MusicTransport? = null

/** A function that opens Spotify: the app through its `spotify:` link, else the web player. */
@Composable
fun rememberOpenMusicApp(): () -> Unit = { openSpotifyApp() }
