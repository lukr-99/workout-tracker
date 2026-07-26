package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.recovery.RecoverySnapshot
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.ExerciseProgressSummary
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.components.BarPoint
import com.lukr99.workout.ui.components.BodyHeatmap
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.components.Sparkline
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.components.VolumeBars
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.Danger
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.Positive
import com.lukr99.workout.ui.theme.TextMid
import com.lukr99.workout.ui.theme.Warning

/** Progress analytics home — KPIs + weekly volume trend + per-exercise strength list. */
@Composable
fun ProgressScreen(
    vm: ProgressViewModel,
    units: UnitSystem,
    onOpenExercise: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Progress", "Strength over time") }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Workouts", state.overview.workouts.toString(), modifier = Modifier.weight(1f))
                StatTile("Volume", Format.volume(state.overview.volumeKg, units), unit = Format.unitLabel(units), modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("PR sets", state.overview.prSets.toString(), modifier = Modifier.weight(1f))
                StatTile("Streak", state.overview.streakWeeks.toString(), unit = "weeks", modifier = Modifier.weight(1f))
            }
        }

        state.recovery?.takeIf { it.muscles.isNotEmpty() }?.let { recovery ->
            item { MuscleRecoveryCard(recovery, units) }
        }

        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Weekly volume", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                VolumeBars(
                    bars = state.weeklyVolume.map { BarPoint(it.label, it.volumeKg) },
                    valueFormat = { Format.volume(it, units) },
                )
            }
        }

        item {
            Text("Exercises", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        if (state.exercises.isEmpty()) {
            item { EmptyHint(if (state.loaded) "Log some sets to see progress." else "Loading…") }
        } else {
            items(state.exercises, key = { it.exerciseId }) { summary ->
                ExerciseRow(summary, units) { onOpenExercise(summary.exerciseId) }
            }
        }
    }
}

/** Lyfta-style Overview card: the front/back heatmap + the least-recovered muscles with weekly sets. */
@Composable
private fun MuscleRecoveryCard(recovery: RecoverySnapshot, units: UnitSystem) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Muscle recovery", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            Text(
                "${recovery.averageReadiness.toInt()}% ready",
                style = MaterialTheme.typography.labelMedium,
                color = readinessTint(recovery.averageReadiness),
            )
        }

        BodyHeatmap(recovery)

        // Least-recovered muscles first — where recent training landed.
        val trained = recovery.muscles
            .filter { it.lastTrainedAtUtc != null }
            .sortedBy { it.readiness }
            .take(6)
        if (trained.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                trained.forEach { m ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.bodyPart, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.width(96.dp))
                        Box(
                            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                Modifier.fillMaxWidth((m.readiness / 100.0).toFloat().coerceIn(0f, 1f))
                                    .height(8.dp).clip(RoundedCornerShape(4.dp))
                                    .background(readinessTint(m.readiness)),
                            )
                        }
                        Text(
                            "  ${m.readiness.toInt()}%",
                            style = Numbers.copy(fontSize = 12.sp), color = TextMid,
                            modifier = Modifier.width(40.dp),
                        )
                        Text(
                            "${m.weeklySetCount.toInt()} sets/wk",
                            style = MaterialTheme.typography.labelSmall, color = TextMid,
                            modifier = Modifier.width(66.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun readinessTint(readiness: Double) = when {
    readiness >= 66 -> Positive
    readiness >= 33 -> Warning
    else -> Danger
}

@Composable
private fun ExerciseRow(summary: ExerciseProgressSummary, units: UnitSystem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(summary.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Est 1RM ", style = MaterialTheme.typography.labelSmall, color = TextMid)
                Text(Format.weightWithUnit(summary.bestE1rmKg, units), style = Numbers.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onBackground)
                if (summary.deltaKg != 0.0) {
                    val up = summary.deltaKg > 0
                    Text(
                        "  ${if (up) "▲" else "▼"} ${Format.weight(kotlin.math.abs(summary.deltaKg), units)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (up) Positive else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Sparkline(
            values = summary.points.map { it.e1rmKg },
            color = Accents.E1rm,
            modifier = Modifier.width(64.dp).height(28.dp),
        )
    }
}
