package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.music.SpotifyController
import com.lukr99.workout.data.music.StubSpotifyController

/** The music controller in scope for the live screens. Provided once at the app root. */
val LocalSpotify: ProvidableCompositionLocal<SpotifyController> =
    staticCompositionLocalOf { StubSpotifyController }

/**
 * The minimal, shared music control (R4) hosted on **both** the live run and live lift screens —
 * deliberately a **small side button**. It connects the [SpotifyController] while shown and renders:
 * - **not connected** (the shipping default): a single round **note** button → Open Spotify;
 * - **connected** (App Remote): a compact previous · play/pause · next row.
 *
 * No player, search, or queue — by design.
 */
@Composable
fun MusicMiniControls(modifier: Modifier = Modifier) {
    val controller = LocalSpotify.current
    val context = LocalContext.current
    val available by controller.available.collectAsState()
    val track by controller.track.collectAsState()

    DisposableEffect(controller) {
        controller.connect(context)
        onDispose { controller.disconnect() }
    }

    val current = track
    if (available && current != null) {
        Row(
            modifier.clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .size(width = 132.dp, height = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", controller::previous)
            TransportButton(
                if (current.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (current.isPlaying) "Pause" else "Play",
                controller::playPause,
                primary = true,
            )
            TransportButton(Icons.Rounded.SkipNext, "Next", controller::next)
        }
    } else {
        // Small, unobtrusive note button — tap to open Spotify.
        Box(
            modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .clickable { controller.openSpotify(context) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote, "Open Spotify",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TransportButton(icon: ImageVector, label: String, onClick: () -> Unit, primary: Boolean = false) {
    val fg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(if (primary) 24.dp else 20.dp))
    }
}

/** Convenience for the app root to inject the controller into the tree. */
@Composable
fun ProvideSpotify(controller: SpotifyController, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpotify provides controller, content = content)
}
