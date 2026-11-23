package com.lukr99.workout.ui.screens

import com.lukr99.workout.ui.i18n.ProgressScreenText
import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.LocalLanguage
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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, CommonText.back.text(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                detail?.name ?: ProgressScreenText.exerciseFallback.text(),
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Chart view: false is the default line, true is one line per place.
        var byGym by rememberSaveable(exerciseId) { mutableStateOf(false) }
        val noneHere = detail != null && detail.points.isEmpty() && detail.canSplitByGym
        val splitView = detail != null && detail.canSplitByGym && (byGym || noneHere)

        if (detail == null || (detail.points.isEmpty() && !noneHere)) {
            EmptyState(Icons.AutoMirrored.Rounded.ShowChart, ProgressScreenText.noSetsTitle.text(), ProgressScreenText.noSetsBody.text())
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
                    ProgressScreenText.trackedPerGym.text("place" to locations.nameOf(detail.scope.locationId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = EmberTheme.colors.textSecondary,
                )
            }
            if (noneHere) {
                Text(
                    ProgressScreenText.noneHere.text("place" to locations.nameOf(detail.scope.locationId)),
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
                StatTile(ProgressScreenText.bestE1rmTile.text(), Format.weight(detail.bestE1rmKg, units), unit = Format.unitLabel(units), modifier = Modifier.weight(1f))
                StatTile(ProgressScreenText.latestE1rmTile.text(), Format.weight(detail.latestE1rmKg, units), unit = Format.unitLabel(units), modifier = Modifier.weight(1f))
                StatTile(ProgressScreenText.sessionsTile.text(), detail.points.size.toString(), modifier = Modifier.weight(1f))
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(ProgressScreenText.estimated1RmTitle.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
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
                Text(ProgressScreenText.volumePerSession.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                VolumeBars(
                    bars = detail.points.takeLast(10).map { BarPoint(Format.shortDate(it.dateMillis), it.volumeKg) },
                    valueFormat = { Format.volume(it, units) },
                )
            }

            records?.let { RecordsCard(it, units) }

            InlineHint(Icons.Rounded.TouchApp, ProgressScreenText.chartHint.text())
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
    val lang = LocalLanguage.current
    val summary = ProgressScreenText.byGymSummaryIntro.get(lang) + " " + detail.series.joinToString(" ") {
        ProgressScreenText.byGymSummaryItem.get(
            lang, it.points.size,
            "name" to it.name,
            "latest" to Format.weight(it.latestE1rmKg, units),
            "best" to Format.weight(it.bestE1rmKg, units),
            "unit" to unit,
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ProgressScreenText.byGymTitle.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        MultiLineChart(
            lines = lines,
            summary = summary,
            valueFormat = { Format.weightWithUnit(it, units) },
        )
        InlineHint(Icons.Rounded.TouchApp, ProgressScreenText.compareGymsHint.text())
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
            Text(ProgressScreenText.records.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(ProgressScreenText.prBadge.text(), style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.success, fontWeight = FontWeight.Bold)
        }

        records.mostReps?.let {
            RecordRow(ProgressScreenText.mostReps.text(), ProgressScreenText.repsCount.text(it.reps), it.source)
        }
        records.heaviestSet?.let {
            RecordRow(ProgressScreenText.heaviestSet.text(), "${Format.weight(it.weightKg, units)} ${Format.unitLabel(units)} × ${it.reps}", it.source)
        }
        records.bestEstimated1Rm?.let {
            RecordRow(ProgressScreenText.bestE1rm.text(), Format.weightWithUnit(it.estimated1RmKg, units), it.source)
        }
        records.bestSetVolume?.let {
            RecordRow(ProgressScreenText.bestSetVolume.text(), "${Format.volume(it.setVolumeKg, units)} ${Format.unitLabel(units)}", it.source)
        }
        records.bestSessionVolume?.let {
            RecordRow(ProgressScreenText.bestSessionVolume.text(), "${Format.volume(it.volumeKg, units)} ${Format.unitLabel(units)} · ${ProgressScreenText.setsCount.text(it.setCount)}", it.source)
        }

        if (records.repMaxes.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(ProgressScreenText.repMaxes.text(), style = MaterialTheme.typography.labelMedium, color = EmberTheme.colors.textSecondary)
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
