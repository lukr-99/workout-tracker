package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.components.BarPoint
import com.lukr99.workout.ui.components.ChartPoint
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ProgressChart
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.components.VolumeBars
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.TextMid

/** Per-exercise progress detail — the spline e1RM trend + per-session volume bars. */
@Composable
fun ProgressDetailScreen(
    vm: ProgressViewModel,
    exerciseId: String,
    units: UnitSystem,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val detail = state.exercises.firstOrNull { it.exerciseId == exerciseId }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                detail?.name ?: "Exercise",
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (detail == null || detail.points.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                EmptyHint("No logged sets for this exercise yet.")
            }
            return
        }

        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Best e1RM", Format.weight(detail.bestE1rmKg, units), unit = Format.unitLabel(units), modifier = Modifier.weight(1f))
                StatTile("Latest e1RM", Format.weight(detail.latestE1rmKg, units), unit = Format.unitLabel(units), modifier = Modifier.weight(1f))
                StatTile("Sessions", detail.points.size.toString(), modifier = Modifier.weight(1f))
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Estimated 1RM", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                ProgressChart(
                    points = detail.points.map { ChartPoint(Format.shortDate(it.dateMillis), it.e1rmKg) },
                    lineColor = Accents.E1rm,
                    valueFormat = { Format.weightWithUnit(it, units) },
                )
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Volume per session", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                VolumeBars(
                    bars = detail.points.takeLast(10).map { BarPoint(Format.shortDate(it.dateMillis), it.volumeKg) },
                    valueFormat = { Format.volume(it, units) },
                )
            }

            Text(
                "Tap and drag the chart to scrub through sessions.",
                style = MaterialTheme.typography.labelSmall, color = TextMid,
            )
        }
    }
}
