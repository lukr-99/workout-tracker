package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSummary
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HomeViewModel
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/** Home dashboard — resume card, quick-start templates, KPIs, recent sessions, Library entry. */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    units: UnitSystem,
    onStart: () -> Unit,
    onStartTemplate: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    val snapshot by vm.snapshot.collectAsState()
    val active by vm.activeSession.collectAsState()
    val templates by vm.templates.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Home", "Your training at a glance") }

        active?.let { session ->
            item { ResumeCard(session, onResume = onStart) }
        }

        item {
            QuickStart(
                templates = templates,
                onEmpty = onStart,
                onTemplate = onStartTemplate,
                onOpenLibrary = onOpenLibrary,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "This week", snapshot.consistency.workoutsLast7Days.toString(),
                    unit = "workouts", modifier = Modifier.weight(1f),
                )
                StatTile(
                    "Streak", snapshot.analytics.currentWeeklyStreak.toString(),
                    unit = "weeks", modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Total volume", Format.volume(snapshot.analytics.totalVolumeKg, units),
                    unit = Format.unitLabel(units), modifier = Modifier.weight(1f),
                )
                StatTile(
                    "Workouts", snapshot.analytics.totalCompletedWorkouts.toString(),
                    unit = "all-time", modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text("Recent", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        if (snapshot.recentWorkouts.isEmpty()) {
            item { EmptyHint("No workouts yet — hit Start to log your first.") }
        } else {
            items(snapshot.recentWorkouts, key = { it.id }) { summary ->
                RecentRow(summary, units) { onOpenSession(summary.id) }
            }
        }
    }
}

@Composable
private fun ResumeCard(session: WorkoutSession, onResume: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .clickable(onClick = onResume).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "IN PROGRESS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(session.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "${session.entries.size} exercises · started ${Format.relativeDay(session.startedAtUtc)}",
                style = MaterialTheme.typography.labelSmall, color = TextMid,
            )
        }
        Icon(
            Icons.Rounded.PlayArrow, "Resume",
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun QuickStart(
    templates: List<WorkoutTemplate>,
    onEmpty: () -> Unit,
    onTemplate: (String) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Quick start", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f),
            )
            Text(
                "Library", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onOpenLibrary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onEmpty).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text("  Empty workout", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        }
        if (templates.isEmpty()) {
            EmptyHint("Create a template in the Library for one-tap starts.")
        } else {
            templates.take(4).forEach { template ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { onTemplate(template.id) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(template.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                        Text("${template.exercises.size} exercises", style = MaterialTheme.typography.labelSmall, color = TextMid)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TextMid)
                }
            }
        }
    }
}

@Composable
private fun RecentRow(summary: WorkoutSessionSummary, units: UnitSystem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(summary.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "${Format.relativeDay(summary.completedDateUtc ?: summary.startedAtUtc)} · ${summary.sessionTypeLabel} · ${summary.exerciseCount} exercises",
                style = MaterialTheme.typography.labelSmall, color = TextMid,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Format.volume(summary.totalVolumeKg, units),
                style = Numbers.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onBackground,
            )
            Text(Format.unitLabel(units), style = MaterialTheme.typography.labelSmall, color = TextMid)
        }
    }
}
