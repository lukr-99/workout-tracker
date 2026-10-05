package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.TrainingDay
import com.lukr99.workout.domain.TrainingWeek
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HomeRecent
import com.lukr99.workout.ui.HomeViewModel
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Home: what is in progress, how the week is going, and the fastest ways to start. */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    units: UnitSystem,
    onResume: () -> Unit,
    onStartTemplate: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenRun: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val snapshot by vm.snapshot.collectAsState()
    val active by vm.activeSession.collectAsState()
    val templates by vm.templates.collectAsState()
    val week by vm.week.collectAsState()
    val recent by vm.recent.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Today", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM"))) {
                RoundIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
            }
        }
        active?.let { session -> item { ResumeCard(session, onResume) } }
        item { SectionLabel("This week") { Streak(snapshot.consistency.currentWeeklyStreak) } }
        item { WeekCard(week, units) }
        if (templates.isNotEmpty()) {
            item { SectionLabel("Start from a template") { TextLink("All templates", onOpenTemplates) } }
            item { TemplateCard(templates.take(4), onStartTemplate, onOpenTemplate) }
        }
        item { SectionLabel("Recent") {} }
        if (recent.isEmpty()) {
            item { EmptyHint("Nothing yet. Press Start to log your first workout or run.") }
        } else {
            items(recent, key = { (if (it.isRun) "run-" else "lift-") + it.id }) { row ->
                RecentRow(row, units) { if (row.isRun) onOpenRun(row.id) else onOpenSession(row.id) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = EmberTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = EmberTheme.colors.primaryText,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun Streak(weeks: Int) {
    if (weeks <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.LocalFireDepartment, null, tint = EmberTheme.colors.primaryText, modifier = Modifier.size(16.dp))
        Text(
            if (weeks == 1) "1 week streak" else "$weeks week streak",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = EmberTheme.colors.primaryText,
        )
    }
}

@Composable
private fun ResumeCard(session: WorkoutSession, onResume: () -> Unit) {
    val colors = EmberTheme.colors
    val minutes = ((System.currentTimeMillis() - session.startedAtUtc) / 60_000).coerceAtLeast(0)
    val finished = session.entries.count { it.completedAtUtc != null }
    val doneSets = session.entries.sumOf { e -> e.strengthSets.count { it.performedAtUtc != null } }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.primarySoft)
            .border(1.dp, colors.primary.copy(alpha = 0.45f), shape)
            .clickable(onClick = onResume).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "IN PROGRESS · $minutes MIN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.primaryText,
            )
            Text(session.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(
                "$finished of ${session.entries.size} exercises · $doneSets sets done",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
        }
        Row(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(colors.primary)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
            Text("Resume", color = colors.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeekCard(week: TrainingWeek, units: UnitSystem) {
    val colors = EmberTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            week.days.forEach { day -> DayMark(day, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            WeekTotal(week.workouts.toString(), "", if (week.workouts == 1) "Workout" else "Workouts", Modifier.weight(1f))
            WeekTotal(Format.volume(week.liftedKg, units), Format.unitLabel(units), "Lifted", Modifier.weight(1f))
            WeekTotal(shortDistance(week.runMeters, units), if (units == UnitSystem.Imperial) "mi" else "km", "Run", Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayMark(day: TrainingDay, modifier: Modifier) {
    val colors = EmberTheme.colors
    val name = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val what = when {
        day.lifted && day.ran -> "workout and run"
        day.lifted -> "workout"
        day.ran -> "run"
        else -> "rest"
    }
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "$name: $what" + if (day.isToday) ", today" else "" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val circle = Modifier.size(34.dp).clip(CircleShape)
        when {
            day.lifted -> Box(circle.background(colors.primary), contentAlignment = Alignment.Center) {
                Icon(if (day.ran) Icons.AutoMirrored.Rounded.DirectionsRun else Icons.Rounded.FitnessCenter, null, tint = colors.onPrimary, modifier = Modifier.size(17.dp))
            }
            day.ran -> Box(circle.background(colors.primarySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.DirectionsRun, null, tint = colors.primaryText, modifier = Modifier.size(17.dp))
            }
            day.isToday -> Box(circle.border(2.dp, colors.primary.copy(alpha = 0.55f), CircleShape))
            else -> Box(circle.background(colors.surfaceRaised))
        }
        Text(
            day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.SemiBold,
            color = if (day.isToday) colors.textPrimary else colors.textSecondary,
        )
    }
}

@Composable
private fun WeekTotal(value: String, unit: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Numbers.copy(fontSize = 24.sp), color = EmberTheme.colors.textPrimary)
            if (unit.isNotBlank()) {
                Text(" $unit", style = MaterialTheme.typography.labelLarge, color = EmberTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
    }
}

@Composable
private fun TemplateCard(templates: List<WorkoutTemplate>, onStart: (String) -> Unit, onOpen: (String) -> Unit) {
    val colors = EmberTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp)),
    ) {
        templates.forEachIndexed { index, template ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                Modifier.fillMaxWidth().clickable(onClickLabel = "Preview ${template.name}") { onOpen(template.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        template.exercises.sortedBy { it.sortOrder }.joinToString { it.exerciseName }
                            .ifBlank { "No exercises yet" },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(colors.surfaceRaised)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(role = Role.Button) { onStart(template.id) }
                        .semantics { contentDescription = "Start ${template.name}" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = colors.primaryText, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentRow(row: HomeRecent, units: UnitSystem, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (row.isRun) Icons.AutoMirrored.Rounded.DirectionsRun else Icons.Rounded.FitnessCenter,
                contentDescription = if (row.isRun) "Run" else "Workout",
                tint = colors.textSecondary,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Format.relativeDay(row.atUtc), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
        val (value, unit) = if (row.isRun) {
            Format.distance(row.amount, units).split(' ').let { it[0] to it[1] }
        } else {
            Format.volume(row.amount, units) to Format.unitLabel(units)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Numbers.copy(fontSize = 19.sp), color = colors.textPrimary)
            Text(" $unit", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** Distance with one decimal and no trailing ".0", for the week totals: "7.2", "12". */
private fun shortDistance(meters: Double, units: UnitSystem): String {
    val value = if (units == UnitSystem.Imperial) meters / 1_609.344 else meters / 1_000.0
    return "%.1f".format(value).replace(Regex("[.,]0$"), "")
}
