package com.lukr99.workout.data.music

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shipping default — **Open Spotify** only. No App Remote SDK / client id required, so it compiles and
 * runs everywhere; [available] is always false so only the open button shows.
 */
object StubSpotifyController : SpotifyController {
    private val _available = MutableStateFlow(false)
    override val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _track = MutableStateFlow<MusicTrack?>(null)
    override val track: StateFlow<MusicTrack?> = _track.asStateFlow()

    override fun connect(context: Context) = Unit
    override fun disconnect() = Unit
    override fun playPause() = Unit
    override fun next() = Unit
    override fun previous() = Unit

    override fun openSpotify(context: Context) = openSpotifyApp(context)
}
