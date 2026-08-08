package com.lukr99.workout.data.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The currently-playing track, when a music remote is connected. */
data class MusicTrack(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
)

/**
 * Deliberately-small music control seam, shared by the live **run** and live **lift** screens
 * ([com.lukr99.workout.ui.components.MusicMiniControls]). It never embeds a player/search/queue — it
 * only ever offers **Open Spotify**, and, when a Spotify **App Remote** is connected, the current
 * track + play/pause · next · previous.
 *
 * The default binding is [StubSpotifyController]: **Open Spotify works today** (a plain intent, no SDK
 * or credentials), while [available] stays false so the transport UI simply doesn't show. Dropping in
 * a real App Remote implementation is a pure add — implement this interface (see
 * `docs/run-mode/handoff-R4.md`), fill [available]/[track] + the transport calls, and swap
 * [com.lukr99.workout.data.AppContainer.spotify]. The UI lights up with **no UI changes**.
 */
interface SpotifyController {
    /** True once an App Remote is connected — gates the track/transport UI. */
    val available: StateFlow<Boolean>

    /** The current track while connected, else null. */
    val track: StateFlow<MusicTrack?>

    /** Begin/end the App Remote connection (no-ops in the stub). Called on live-screen enter/leave. */
    fun connect(context: Context)
    fun disconnect()

    fun playPause()
    fun next()
    fun previous()

    /** Always available: bring Spotify to the foreground (or the store if it isn't installed). */
    fun openSpotify(context: Context)
}

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

/** Launch the Spotify app; fall back to the Play Store, then the web player. */
internal fun openSpotifyApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
    val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SPOTIFY_PACKAGE"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val started = runCatching { context.startActivity(intent); true }.getOrDefault(false)
    if (!started) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

private const val SPOTIFY_PACKAGE = "com.spotify.music"
