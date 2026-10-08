package com.lukr99.workout.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Split
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/** Only surface an off-route hint once you're clearly off the line, never for GPS wobble. */
internal const val OFF_ROUTE_THRESHOLD_M = 35.0

/**
 * The live run's one bottom card, as in the redesign: the distance big, a badge for the route or
 * auto-pause, then time, average pace and the current split's pace, then Pause and Finish. It sits
 * low so the map above stays clear.
 */
@Composable
internal fun LiveRunCard(
    state: LiveRunState,
    units: UnitSystem,
    routeName: String?,
    offRouteMeters: Double?,
    split: Split?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmberTheme.colors
    val paused = state.phase == com.lukr99.workout.domain.run.RunTracker.Phase.Paused
    Column(
        modifier.clip(RoundedCornerShape(26.dp)).background(colors.surface.copy(alpha = 0.96f)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (value, unit) = Format.distance(state.distanceMeters, units).split(' ').let { it[0] to it.getOrElse(1) { "" } }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(value, style = Numbers.copy(fontSize = 52.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Text(" $unit", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            }
            Badge(state, routeName, offRouteMeters)
        }
        Row(Modifier.fillMaxWidth()) {
            RunMetric("Time", Pace.formatDuration(state.movingSeconds), Modifier.weight(1f))
            RunMetric("Pace", runPaceLabel(state.avgPaceSecPerKm, units), Modifier.weight(1f))
            RunMetric(
                "Split ${split?.index ?: (state.distanceMeters / splitMetersFor(units)).toInt() + 1}",
                split?.let { runPaceLabel(it.paceSecPerKm, units) } ?: "–",
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlButton(
                if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                if (paused) "Resume" else "Pause",
                container = colors.primary,
                content = colors.onPrimary,
                modifier = Modifier.weight(1f),
                onClick = if (paused) onResume else onPause,
            )
            ControlButton(
                Icons.Rounded.Stop,
                "Finish",
                container = colors.surfaceRaised,
                content = colors.textPrimary,
                modifier = Modifier.weight(1f),
                onClick = onFinish,
            )
        }
    }
}

/** Auto-pause first, then the route: on it, or how far off. Off-route is a heads-up, never a warning. */
@Composable
private fun Badge(state: LiveRunState, routeName: String?, offRouteMeters: Double?) {
    val colors = EmberTheme.colors
    val (text, tint) = when {
        state.autoPaused -> "Auto-paused" to colors.primaryText
        routeName == null -> return
        offRouteMeters != null && offRouteMeters > OFF_ROUTE_THRESHOLD_M -> "${offRouteMeters.toInt()} m off route" to colors.textSecondary
        else -> "On route · $routeName" to colors.success
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
internal fun RunMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = Numbers.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold), color = EmberTheme.colors.textPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = EmberTheme.colors.textSecondary)
    }
}

internal fun runPaceLabel(secPerKm: Double, units: UnitSystem): String {
    val perUnit = if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm
    val suffix = if (units == UnitSystem.Imperial) "/mi" else "/km"
    return "${Pace.formatPace(perUnit)} $suffix"
}

internal fun splitMetersFor(units: UnitSystem): Double =
    if (units == UnitSystem.Imperial) Pace.METERS_PER_MILE else Pace.METERS_PER_KM

@Composable
private fun ControlButton(icon: ImageVector, label: String, container: Color, content: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(28.dp)).background(container).clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(22.dp))
        Text("  $label", color = content, fontWeight = FontWeight.Bold)
    }
}
