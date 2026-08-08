package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.music.SpotifyController
import com.lukr99.workout.data.music.StubSpotifyController

/** The music controller in scope for the live screens. Provided once at the app root. */
val LocalSpotify: ProvidableCompositionLocal<SpotifyController> =
    staticCompositionLocalOf { StubSpotifyController }

/**
 * The minimal, shared music control (R4) hosted on **both** the live run and live lift screens. It
 * connects the [SpotifyController] while shown and renders one of two states:
 * - **connected** (App Remote): the current track + previous · play/pause · next;
 * - **otherwise**: a single **Open Spotify** button.
 *
 * No player, search, or queue — by design. With the shipping [StubSpotifyController] only the Open
 * button shows; a real App Remote binding lights up the transport row with no change here.
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
    Row(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))

        if (available && current != null) {
            Column(Modifier.weight(1f)) {
                Text(
                    current.title.ifBlank { "Spotify" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (current.artist.isNotBlank()) {
                    Text(
                        current.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TransportButton(Icons.Rounded.SkipPrevious, "Previous", controller::previous)
            TransportButton(
                if (current.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (current.isPlaying) "Pause" else "Play",
                controller::playPause,
                primary = true,
            )
            TransportButton(Icons.Rounded.SkipNext, "Next", controller::next)
        } else {
            Text(
                "Open Spotify",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clickable { controller.openSpotify(context) },
            )
        }
    }
}

@Composable
private fun TransportButton(icon: ImageVector, label: String, onClick: () -> Unit, primary: Boolean = false) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.layout.Box(
        Modifier.size(34.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(20.dp))
    }
}

/** Convenience for the app root to inject the controller into the tree. */
@Composable
fun ProvideSpotify(controller: SpotifyController, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSpotify provides controller, content = content)
}
