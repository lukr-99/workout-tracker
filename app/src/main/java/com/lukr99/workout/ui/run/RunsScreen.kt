package com.lukr99.workout.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/**
 * Run Mode hub — the tab that replaces History's old slot. Recent runs + saved routes + a prominent
 * start. R0 ships the shell and empty state; recording (R1) and run detail/stats (R2) fill it in.
 */
@Composable
fun RunsScreen(
    vm: RunViewModel,
    units: UnitSystem,
    onStartRun: () -> Unit,
    onOpenRun: (String) -> Unit,
    onPlanRoute: () -> Unit,
    onStartRoute: (String) -> Unit,
) {
    val runs by vm.runs.collectAsState()
    val routes by vm.routes.collectAsState()

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Runs", "Track a run outdoors") }
        item { StartRunButton(onStartRun) }
        item { PlanRouteButton(onPlanRoute) }

        if (routes.isNotEmpty()) {
            item {
                Text(
                    "Saved routes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            items(routes, key = { "route-${it.id}" }) { route ->
                RouteRow(route, units) { onStartRoute(route.id) }
            }
        }

        item {
            Text(
                "Recent runs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (runs.isEmpty()) {
            item { EmptyHint("No runs yet. Tap Start a run to record your first.") }
        } else {
            items(runs, key = { it.id }) { run -> RunRow(run, units) { onOpenRun(run.id) } }
        }
    }
}

@Composable
private fun PlanRouteButton(onPlanRoute: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPlanRoute)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Rounded.Map, null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp),
        )
        Text(
            "Plan a route",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun RouteRow(route: com.lukr99.workout.domain.run.Route, units: UnitSystem, onStart: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onStart).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                route.name.ifBlank { "Route" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text("Tap to run this route", style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
        Text(
            Format.distance(route.distanceMeters, units),
            style = MaterialTheme.typography.labelLarge,
            color = TextMid,
        )
    }
}

@Composable
private fun StartRunButton(onStartRun: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onStartRun)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.DirectionsRun, null,
            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp),
        )
        Text(
            "Start a run",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun RunRow(run: Run, units: UnitSystem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                run.notes.ifBlank { "Run" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(Format.date(run.startedAtUtc), style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Metric(Format.distance(run.distanceMeters, units), "distance")
            Metric(Pace.formatDuration(run.durationSeconds), "time")
            Metric(Pace.formatPace(paceForUnits(run.avgPaceSecPerKm, units)), paceLabel(units))
        }
    }
}

private fun paceForUnits(secPerKm: Double, units: UnitSystem): Double =
    if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm

private fun paceLabel(units: UnitSystem): String = if (units == UnitSystem.Imperial) "/mi" else "/km"

@Composable
private fun Metric(value: String, label: String) {
    Box {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Numbers, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.size(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
    }
}
