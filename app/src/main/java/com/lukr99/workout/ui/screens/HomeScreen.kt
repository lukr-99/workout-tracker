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
import com.lukr99.workout.ui.i18n.HomeText
import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.SettingsText
import com.lukr99.workout.ui.i18n.AppText

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
    /** Claude's proposals waiting for the owner, when the platform has the Claude connector. */
    claudeCard: (@Composable () -> Unit)? = null,
    locationSetupCard: (@Composable () -> Unit)? = null,
    /** False without Run Mode (the web edition): no run total, and the copy talks about workouts only. */
    showRuns: Boolean = true,
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
            ScreenHeader(CommonText.today.text(), localDateText(Format.today(), DateStyle.Header)) {
                RoundIconButton(Icons.Rounded.Settings, SettingsText.title.text(), onOpenSettings)
            }
        }
        updateCard?.let { card -> item(key = "update") { card() } }
        claudeCard?.let { card -> item(key = "claude") { card() } }
        whatsNew?.let { note -> item(key = "whats-new") { WhatsNewCard(note, vm::dismissWhatsNew) } }
        locationSetupCard?.let { card -> item(key = "location-setup") { card() } }
        active?.let { session -> item { ResumeCard(session, onResume) } }
        item { SectionLabel(HomeText.thisWeek.text()) { Streak(snapshot.consistency.currentWeeklyStreak) } }
        item { WeekCard(week, units, showRuns) { if (it.isRun) onOpenRun(it.id) else onOpenSession(it.id) } }
        if (templates.isNotEmpty()) {
            item { SectionLabel(HomeText.startFromTemplate.text()) { TextLink(HomeText.allTemplates.text(), onOpenTemplates) } }
            item { TemplateCard(templates.take(4), lastDone, onStartTemplate, onOpenTemplate) }
        }
        item { SectionLabel(HomeText.recent.text()) {} }
        if (recent.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.History,
                    if (showRuns) HomeText.emptyTitleBoth.text() else HomeText.emptyTitleWorkouts.text(),
                    if (showRuns) HomeText.emptyBodyBoth.text() else HomeText.emptyBodyWorkouts.text(),
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
            HomeText.weekStreak.text(weeks),
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
                HomeText.inProgress.text("n" to minutes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.primaryText,
            )
            Text(session.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(
                HomeText.exercisesOf.text(session.entries.size, "done" to finished) + " · " + HomeText.setsDone.text(doneSets),
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
            Text(AppText.resume.text(), color = colors.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The week strip and its totals. Tapping a day opens its workout or run, or lists them first when
 * the day has more than one. Rest days do nothing.
 */
@Composable
internal fun WeekCard(week: TrainingWeek, units: UnitSystem, showRuns: Boolean = true, onOpen: (DayActivity) -> Unit) {
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
            WeekTotal(week.workouts.toString(), "", HomeText.totalWorkouts.text(week.workouts), Modifier.weight(1f))
            WeekTotal(Format.volume(week.liftedKg, units), Format.unitLabel(units), HomeText.totalLifted.text(), Modifier.weight(1f))
            if (showRuns) {
                WeekTotal(shortDistance(week.runMeters, units), if (units == UnitSystem.Imperial) "mi" else "km", HomeText.totalRun.text(), Modifier.weight(1f))
            }
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
        day.lifted && day.ran -> HomeText.dayWorkoutAndRun.text()
        day.lifted -> HomeText.dayWorkout.text()
        day.ran -> HomeText.dayRun.text()
        else -> HomeText.dayRest.text()
    }
    val todaySuffix = HomeText.dayToday.text()
    val clickLabel = when {
        day.activities.size > 1 -> HomeText.chooseActivity.text()
        day.ran -> HomeText.openRun.text()
        else -> HomeText.openWorkout.text()
    } + ", " + Format.longDay(day.date)
    val tap = if (day.trained) {
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier.testTag(AppTags.homeDay(day.date)).heightIn(min = 48.dp).then(tap).padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$name: $what" + if (day.isToday) todaySuffix else "" },
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
            val startLabel = HomeText.startName.text("name" to template.name)
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                Modifier.fillMaxWidth().clickable(onClickLabel = HomeText.previewName.text("name" to template.name)) { onOpen(template.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(template.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        listOfNotNull(
                            lastDone[template.id]?.let { Format.relativeDay(it) },
                            template.exercises.sortedBy { it.sortOrder }.joinToString { it.exerciseName }.ifBlank { HomeText.noExercisesYet.text() },
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
                        .semantics { contentDescription = startLabel },
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
                contentDescription = if (row.isRun) HomeText.run.text() else HomeText.workout.text(),
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
