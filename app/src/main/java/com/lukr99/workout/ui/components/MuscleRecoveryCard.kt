package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.recovery.MuscleRecovery
import com.lukr99.workout.domain.recovery.RecoveryState
import com.lukr99.workout.domain.recovery.RecoverySnapshot
import com.lukr99.workout.domain.recovery.WeeklyVolumeStatus
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The Progress body map card. Recovery colours each muscle by how recovered it is now; Weekly sets
 * colours it by hard sets in the last seven days against the target range. The info button explains
 * both and lists the sources.
 */
@Composable
fun MuscleRecoveryCard(
    recovery: RecoverySnapshot,
    modifier: Modifier = Modifier,
    initialMode: MuscleMapMode = MuscleMapMode.Recovery,
) {
    val colors = EmberTheme.colors
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val target = recovery.config.weeklyTargetSets
    val groups = recovery.allGroups()

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(colors.surface).border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .testTag(AppTags.MUSCLE_MAP),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Muscle recovery", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            IconButton(onClick = { showInfo = true }, modifier = Modifier.testTag(AppTags.MUSCLE_MAP_INFO)) {
                Icon(Icons.Outlined.Info, contentDescription = "How this is calculated", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (mode == MuscleMapMode.Recovery) {
                    "${recovery.averageReadiness.roundToInt()}% ready"
                } else {
                    "${groups.count { it.weeklyStatus == WeeklyVolumeStatus.InRange }} of ${groups.size} in range"
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                            )
        }

        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MuscleMapMode.entries.forEach { option ->
                    FilterChip(
                        option.label,
                        selected = mode == option,
                        onClick = { mode = option },
                        modifier = Modifier.testTag(AppTags.muscleMapMode(option)),
                    )
                }
            }

            BodyHeatmap(
                tintOf = { group ->
                    val muscle = recovery.forGroup(group)
                    when (mode) {
                        MuscleMapMode.Recovery -> muscle?.let { MuscleTints.recovery(it.state, colors) }
                        MuscleMapMode.WeeklySets -> MuscleTints.weekly(muscle?.weeklyStatus ?: WeeklyVolumeStatus.None, colors)
                    }
                },
            )

            Legend(
                when (mode) {
                    MuscleMapMode.Recovery -> RecoveryState.entries.map { MuscleTints.recovery(it, colors) to stateLabel(it) }
                    MuscleMapMode.WeeklySets -> listOf(
                        MuscleTints.weekly(WeeklyVolumeStatus.Below, colors)!! to "Below ${sets(target.start)}",
                        colors.success to "${sets(target.start)} to ${sets(target.endInclusive)} sets",
                        colors.violet to "Above ${sets(target.endInclusive)}",
                    )
                },
            )

            when (mode) {
                MuscleMapMode.Recovery -> {
                    val trained = recovery.muscles.filter { it.lastTrainedAtUtc != null }.sortedBy { it.readiness }
                    if (trained.isEmpty()) {
                        Text("No sets in the last two weeks.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    trained.forEach { RecoveryRow(it, recovery.calculatedAtUtc) }
                }
                MuscleMapMode.WeeklySets -> groups.forEach { WeeklyRow(it, target) }
            }
        }
    }

    if (showInfo) RecoveryInfoSheet(recovery.config, onDismiss = { showInfo = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(items: List<Pair<Color, String>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(label, style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun RecoveryRow(m: MuscleRecovery, nowUtc: Long) {
    val colors = EmberTheme.colors
    val tint = MuscleTints.recovery(m.state, colors)
    MuscleRow(m.bodyPart, AppTags.muscleRow(m.bodyPart)) {
        Bar(fraction = m.readiness / 100.0, tint = tint, modifier = Modifier.weight(1f))
        Text(
            "${m.readiness.roundToInt()}%",
            style = Numbers.copy(fontSize = 12.sp), color = colors.textSecondary,
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
        )
        Text(
            readyText(m, nowUtc),
            style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1,
            modifier = Modifier.width(76.dp),
        )
    }
}

@Composable
private fun WeeklyRow(m: MuscleRecovery, target: ClosedFloatingPointRange<Double>) {
    val colors = EmberTheme.colors
    val status = m.weeklyStatus ?: WeeklyVolumeStatus.None
    val tint = MuscleTints.weekly(status, colors) ?: colors.textTertiary
    val scale = maxOf(target.endInclusive * 1.25, m.weeklySetCount)
    MuscleRow(m.bodyPart, AppTags.muscleRow(m.bodyPart)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Bar(fraction = m.weeklySetCount / scale, tint = tint, modifier = Modifier.fillMaxWidth())
            // The target range, as a thin mark under the bar.
            Row(Modifier.fillMaxWidth().height(2.dp)) {
                Spacer(Modifier.weight(target.start.toFloat().coerceAtLeast(0.01f)))
                Box(Modifier.weight((target.endInclusive - target.start).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(colors.textTertiary))
                Spacer(Modifier.weight((scale - target.endInclusive).toFloat().coerceAtLeast(0.01f)))
            }
        }
        Text(
            "${sets(m.weeklySetCount)} sets",
            style = Numbers.copy(fontSize = 12.sp), color = colors.textSecondary, maxLines = 1,
            modifier = Modifier.width(64.dp).padding(start = 8.dp),
        )
        Text(
            weeklyLabel(status), style = MaterialTheme.typography.labelSmall, maxLines = 1,
            color = if (status == WeeklyVolumeStatus.InRange || status == WeeklyVolumeStatus.Above) tint else colors.textSecondary,
            modifier = Modifier.width(60.dp),
        )
    }
}

@Composable
private fun MuscleRow(name: String, tag: String, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(
            name, style = MaterialTheme.typography.bodyMedium, color = EmberTheme.colors.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(88.dp),
        )
        content()
    }
}

@Composable
private fun Bar(fraction: Double, tint: Color, modifier: Modifier) {
    Box(modifier.height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(
            Modifier.fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(4.dp)).background(tint),
        )
    }
}

internal fun stateLabel(state: RecoveryState): String = when (state) {
    RecoveryState.Ready -> "Ready"
    RecoveryState.Recovering -> "Recovering"
    RecoveryState.Fatigued -> "Fatigued"
}

private fun weeklyLabel(status: WeeklyVolumeStatus): String = when (status) {
    WeeklyVolumeStatus.None -> "None"
    WeeklyVolumeStatus.Below -> "Below"
    WeeklyVolumeStatus.InRange -> "In range"
    WeeklyVolumeStatus.Above -> "Above"
}

/** "Ready", or how long until the muscle reaches the ready level, like "Ready in 20 h". */
private fun readyText(m: MuscleRecovery, nowUtc: Long): String {
    val readyAt = m.readyAtUtc ?: return stateLabel(m.state)
    val hours = ceil((readyAt - nowUtc) / 3_600_000.0).toInt()
    return when {
        hours <= 0 -> "Ready"
        hours < 48 -> "Ready in $hours h"
        else -> "Ready in ${ceil(hours / 24.0).toInt()} d"
    }
}

/** A set count with at most one decimal, like 12 or 7.5. */
internal fun sets(value: Double): String {
    val rounded = (value * 2).roundToInt() / 2.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
