package com.lukr99.workout.ui.screens

import com.lukr99.workout.ui.i18n.ProgressScreenText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
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
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.ExerciseProgressSummary
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.components.BarPoint
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.EmptyState
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.MuscleRecoveryCard
import com.lukr99.workout.ui.components.Sparkline
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.components.VolumeBars
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

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
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val month = state.context.workoutsThisMonth
                StatTile(
                    ProgressScreenText.workouts.text(), state.overview.workouts.toString(), modifier = Modifier.weight(1f).fillMaxHeight(),
                    delta = if (month > 0) ProgressScreenText.thisMonth.text("n" to month) else ProgressScreenText.noneThisMonth.text(),
                    deltaPositive = if (month > 0) true else null,
                )
                val change = state.context.volumeChangePercent
                StatTile(
                    ProgressScreenText.volume4Weeks.text(), Format.volume(state.context.volumeLast4WeeksKg, units), unit = Format.unitLabel(units), modifier = Modifier.weight(1f).fillMaxHeight(),
                    delta = change?.let { ProgressScreenText.volumeVsBefore.text("change" to if (it >= 0) "+$it" else "$it") } ?: ProgressScreenText.last4Weeks.text(),
                    deltaPositive = change?.let { it >= 0 },
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(ProgressScreenText.prSets.text(), state.context.prSetsLast30Days.toString(), modifier = Modifier.weight(1f).fillMaxHeight(), delta = ProgressScreenText.last30Days.text())
                StatTile(
                    ProgressScreenText.streak.text(), state.overview.streakWeeks.toString(), unit = ProgressScreenText.weekShort.text(), modifier = Modifier.weight(1f).fillMaxHeight(),
                    delta = state.context.bestStreakWeeks.takeIf { it > 0 }?.let { ProgressScreenText.bestStreak.text("n" to it) },
                )
            }
        }

        state.recovery?.takeIf { it.muscles.isNotEmpty() }?.let { recovery ->
            item { MuscleRecoveryCard(recovery) }
        }

        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(EmberTheme.colors.surface).border(1.dp, EmberTheme.colors.border, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(ProgressScreenText.weeklyVolume.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    Text(
                        ProgressScreenText.volumeThisWeek.text("value" to Format.volume(state.context.volumeThisWeekKg, units), "unit" to Format.unitLabel(units)),
                        style = MaterialTheme.typography.labelLarge,
                        color = EmberTheme.colors.textSecondary,
                    )
                }
                VolumeBars(
                    bars = state.weeklyVolume.map { BarPoint(it.label, it.volumeKg) },
                    valueFormat = { Format.volume(it, units) },
                )
            }
        }

        item {
            Text(ProgressScreenText.exercises.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        if (state.exercises.isEmpty()) {
            item {
                if (state.loaded) {
                    EmptyState(Icons.AutoMirrored.Rounded.ShowChart, ProgressScreenText.emptyTitle.text(), ProgressScreenText.emptyBody.text())
                } else {
                    EmptyHint(ProgressScreenText.loading.text())
                }
            }
        } else {
            items(state.exercises, key = { it.exerciseId }) { summary ->
                ExerciseRow(summary, units) { onOpenExercise(summary.exerciseId) }
            }
        }
    }
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
            if (summary.points.isEmpty()) {
                Text(ProgressScreenText.noWorkoutsAtGym.text(), style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
            } else Row(verticalAlignment = Alignment.Bottom) {
                Text(ProgressScreenText.estimated1Rm.text() + " ", style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
                Text(Format.weightWithUnit(summary.bestE1rmKg, units), style = Numbers.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onBackground)
                if (summary.deltaKg != 0.0) {
                    val up = summary.deltaKg > 0
                    Text(
                        "  ${if (up) "▲" else "▼"} ${Format.weight(kotlin.math.abs(summary.deltaKg), units)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (up) EmberTheme.colors.success else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Sparkline(
            values = summary.points.map { it.e1rmKg },
            color = EmberTheme.colors.violet,
            modifier = Modifier.width(64.dp).height(28.dp),
        )
    }
}
