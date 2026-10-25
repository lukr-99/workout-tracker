package com.lukr99.workout.ui.screens

import com.lukr99.workout.domain.nowMillis
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
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.DayActivity
import com.lukr99.workout.domain.localDecimal
import com.lukr99.workout.domain.TrainingDay
import com.lukr99.workout.domain.TrainingWeek
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.HomeRecent
import com.lukr99.workout.ui.HomeViewModel
import com.lukr99.workout.ui.components.DateStyle
import com.lukr99.workout.ui.components.DayActivitySheet
import com.lukr99.workout.ui.components.EmptyState
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.RoundIconButton
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.components.localDateText
import com.lukr99.workout.ui.components.weekdayName
import com.lukr99.workout.ui.components.WhatsNewCard
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

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
    /** The "new version ready" card from the automatic update check, when there is one. */
    updateCard: (@Composable () -> Unit)? = null,
    locationSetupCard: (@Composable () -> Unit)? = null,
) {
    val snapshot by vm.snapshot.collectAsState()
    val active by vm.activeSession.collectAsState()
    val templates by vm.templates.collectAsState()
    val lastDone by vm.templateLastDone.collectAsState()
    val week by vm.week.collectAsState()
    val recent by vm.recent.collectAsState()
    val whatsNew by vm.whatsNew.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader("Today", localDateText(Format.today(), DateStyle.Header)) {
                RoundIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
            }
        }
        updateCard?.let { card -> item(key = "update") { card() } }
        whatsNew?.let { note -> item(key = "whats-new") { WhatsNewCard(note, vm::dismissWhatsNew) } }
        locationSetupCard?.let { card -> item(key = "location-setup") { card() } }
        active?.let { session -> item { ResumeCard(session, onResume) } }
        item { SectionLabel("This week") { Streak(snapshot.consistency.currentWeeklyStreak) } }
        item { WeekCard(week, units) { if (it.isRun) onOpenRun(it.id) else onOpenSession(it.id) } }
        if (templates.isNotEmpty()) {
            item { SectionLabel("Start from a template") { TextLink("All templates", onOpenTemplates) } }
            item { TemplateCard(templates.take(4), lastDone, onStartTemplate, onOpenTemplate) }
        }
        item { SectionLabel("Recent") {} }
        if (recent.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.History,
                    "No workouts or runs yet",
                    "Tap the orange Start button below to log a workout or record a run.",
                )
            }
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
            text.uppercase(),
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
    val minutes = ((nowMillis() - session.startedAtUtc) / 60_000).coerceAtLeast(0)
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

/**
 * The week strip and its totals. Tapping a day opens its workout or run, or lists them first when
 * the day has more than one. Rest days do nothing.
 */
@Composable
internal fun WeekCard(week: TrainingWeek, units: UnitSystem, onOpen: (DayActivity) -> Unit) {
    val colors = EmberTheme.colors
    var choosing by remember { mutableStateOf<TrainingDay?>(null) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            week.days.forEach { day ->
                DayMark(day, Modifier.weight(1f)) {
                    if (day.activities.size == 1) onOpen(day.activities.single()) else choosing = day
                }
            }
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
    choosing?.let { day ->
        DayActivitySheet(
            title = Format.longDay(day.date),
            activities = day.activities,
            onOpen = { choosing = null; onOpen(it) },
            onDismiss = { choosing = null },
        )
    }
}

@Composable
private fun DayMark(day: TrainingDay, modifier: Modifier, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val name = weekdayName(day.date.dayOfWeek, narrow = false)
    val what = when {
        day.lifted && day.ran -> "workout and run"
        day.lifted -> "workout"
        day.ran -> "run"
        else -> "rest"
    }
    val clickLabel = when {
        day.activities.size > 1 -> "Choose a workout or run"
        day.ran -> "Open run"
        else -> "Open workout"
    } + ", " + Format.longDay(day.date)
    val tap = if (day.trained) {
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier.testTag(AppTags.homeDay(day.date)).heightIn(min = 48.dp).then(tap).padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$name: $what" + if (day.isToday) ", today" else "" },
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
            weekdayName(day.date.dayOfWeek, narrow = true),
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
private fun TemplateCard(templates: List<WorkoutTemplate>, lastDone: Map<String, Long>, onStart: (String) -> Unit, onOpen: (String) -> Unit) {
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
                        listOfNotNull(
                            lastDone[template.id]?.let { Format.relativeDay(it) },
                            template.exercises.sortedBy { it.sortOrder }.joinToString { it.exerciseName }.ifBlank { "No exercises yet" },
                        ).joinToString(" · "),
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
    return localDecimal(value, 1).replace(Regex("[.,]0$"), "")
}
