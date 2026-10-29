package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.records.ExerciseRecords
import com.lukr99.workout.domain.records.RecordSource
import com.lukr99.workout.domain.nameOf
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.ExerciseProgressSummary
import com.lukr99.workout.ui.components.BarPoint
import com.lukr99.workout.ui.components.ChartLine
import com.lukr99.workout.ui.components.MultiLineChart
import com.lukr99.workout.ui.components.SegmentedControl
import com.lukr99.workout.ui.components.TimedValue
import com.lukr99.workout.ui.theme.SeriesColors
import com.lukr99.workout.ui.components.ChartPoint
import com.lukr99.workout.ui.components.EmptyState
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.InlineHint
import com.lukr99.workout.ui.components.ProgressChart
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.components.VolumeBars
import com.lukr99.workout.ui.theme.EmberTheme

/** Per-exercise progress detail — the spline e1RM trend + per-session volume bars. */
@Composable
fun ProgressDetailScreen(
    vm: ProgressViewModel,
    exerciseId: String,
    units: UnitSystem,
    onBack: () -> Unit,
    locations: List<com.lukr99.workout.domain.Location> = emptyList(),
) {
    val state by vm.state.collectAsState()
    val records by vm.records.collectAsState()
    val detail = state.exercises.firstOrNull { it.exerciseId == exerciseId }
    LaunchedEffect(exerciseId) {
        // Opened from a live workout, the Progress tab may not have loaded its data yet.
        if (!vm.state.value.loaded) vm.refresh()
        vm.loadRecords(exerciseId)
    }

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

        // Chart view: false is the default line, true is one line per place.
        var byGym by rememberSaveable(exerciseId) { mutableStateOf(false) }
        val noneHere = detail != null && detail.points.isEmpty() && detail.canSplitByGym
        val splitView = detail != null && detail.canSplitByGym && (byGym || noneHere)

        if (detail == null || (detail.points.isEmpty() && !noneHere)) {
            EmptyState(Icons.AutoMirrored.Rounded.ShowChart, "No sets logged yet", "Log this exercise in a workout and its chart and records show up here.")
            return
        }

        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (detail.canSplitByGym) {
                SegmentedControl(
                    options = listOf(false, true),
                    selected = splitView,
                    onSelect = { byGym = it },
                    label = { if (it) detail.splitViewLabel else detail.defaultViewLabel },
                )
            }
            if (detail.scope.perLocation && !splitView) {
                Text(
                    "Tracked per gym. Showing ${locations.nameOf(detail.scope.locationId)} only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmberTheme.colors.textSecondary,
                )
            }
            if (noneHere) {
                Text(
                    "No workouts at ${locations.nameOf(detail.scope.locationId)} yet. Here is every gym.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmberTheme.colors.textSecondary,
                )
            }
            if (splitView) {
                GymLinesCard(detail, units)
                records?.let { RecordsCard(it, units) }
                return@Column
            }
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
                    lineColor = EmberTheme.colors.violet,
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

            records?.let { RecordsCard(it, units) }

            InlineHint(Icons.Rounded.TouchApp, "Tap or drag along the chart to see each workout.")
        }
    }
}

/** The e1RM of every place as its own line, with a legend that reads out the value under the finger. */
@Composable
private fun GymLinesCard(detail: ExerciseProgressSummary, units: UnitSystem) {
    val colors = EmberTheme.colors
    val unit = Format.unitLabel(units)
    val lines = detail.series.mapIndexed { index, series ->
        ChartLine(
            name = series.name,
            color = SeriesColors.of(colors, index, isUnknown = series.locationId == null),
            points = series.points.map { TimedValue(it.dateMillis, it.e1rmKg) },
            dashed = series.locationId == null || index >= 5,
        )
    }
    val summary = "Estimated 1RM by gym. " + detail.series.joinToString(" ") {
        val count = it.points.size
        "${it.name}: $count ${if (count == 1) "workout" else "workouts"}, latest ${Format.weight(it.latestE1rmKg, units)} $unit, best ${Format.weight(it.bestE1rmKg, units)} $unit."
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Estimated 1RM by gym", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        MultiLineChart(
            lines = lines,
            summary = summary,
            valueFormat = { Format.weightWithUnit(it, units) },
        )
        InlineHint(Icons.Rounded.TouchApp, "Tap or drag along the chart to compare gyms on a date.")
    }
}

/** Personal records from `insights.records` — headline bests + the 1–12 rep-max table (Lyfta-style). */
@Composable
private fun RecordsCard(records: ExerciseRecords, units: UnitSystem) {
    val hasAny = records.heaviestSet != null || records.repMaxes.isNotEmpty() || records.mostReps != null
    if (!hasAny) return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Records", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("PR", style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.success, fontWeight = FontWeight.Bold)
        }

        records.mostReps?.let {
            RecordRow("Most reps", "${it.reps} reps", it.source)
        }
        records.heaviestSet?.let {
            RecordRow("Heaviest set", "${Format.weight(it.weightKg, units)} ${Format.unitLabel(units)} × ${it.reps}", it.source)
        }
        records.bestEstimated1Rm?.let {
            RecordRow("Best e1RM", Format.weightWithUnit(it.estimated1RmKg, units), it.source)
        }
        records.bestSetVolume?.let {
            RecordRow("Best set volume", "${Format.volume(it.setVolumeKg, units)} ${Format.unitLabel(units)}", it.source)
        }
        records.bestSessionVolume?.let {
            RecordRow("Best session volume", "${Format.volume(it.volumeKg, units)} ${Format.unitLabel(units)} · ${it.setCount} sets", it.source)
        }

        if (records.repMaxes.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text("Rep maxes", style = MaterialTheme.typography.labelMedium, color = EmberTheme.colors.textSecondary)
            records.repMaxes.sortedBy { it.reps }.forEach { rm ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${rm.reps}RM", style = MaterialTheme.typography.bodyMedium, color = EmberTheme.colors.textSecondary, modifier = Modifier.width(52.dp))
                    Text(
                        "${Format.weight(rm.weightKg, units)} ${Format.unitLabel(units)}",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(Format.shortDate(rm.source.dateUtc), style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String, source: RecordSource) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = EmberTheme.colors.textSecondary)
            Text(Format.shortDate(source.dateUtc), style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}
