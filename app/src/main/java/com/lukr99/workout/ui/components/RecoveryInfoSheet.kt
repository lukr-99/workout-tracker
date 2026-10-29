package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.recovery.MuscleGroup
import com.lukr99.workout.domain.recovery.RecoveryConfig
import com.lukr99.workout.domain.recovery.WeeklyVolumeStatus
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.EmberTheme
import kotlin.math.roundToInt

/**
 * Explains the muscle map in plain words: how fatigue and weekly sets are counted, what the colours
 * mean, and the research behind it. The numbers come from [config], so the text follows the model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryInfoSheet(config: RecoveryConfig, onDismiss: () -> Unit) {
    val colors = EmberTheme.colors
    val uriHandler = LocalUriHandler.current
    val target = config.weeklyTargetSets

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            // A bottom sheet is its own window, so it needs testTagsAsResourceId again for its tags.
            Modifier.exposeTestTags().testTag(AppTags.MUSCLE_MAP_INFO_SHEET)
                .fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "How the muscle map works",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            Heading("Recovery")
            Body(
                "Each working set adds fatigue to the muscles it trains. The main muscle gets a full set " +
                    "and each secondary muscle gets ${share(config.secondaryMuscleContribution)} a set. " +
                    "Warm-ups do not count.",
            )
            Body(
                "Sets close to failure count more. A set to failure counts as ${config.maxEffort} sets and an " +
                    "easy set as ${config.minEffort}. Without reps in reserve or RPE, a set counts as 1. " +
                    "Negatives and slow lowering count a bit more, because they cause more muscle damage.",
            )
            Body(
                "Fatigue fades over time. Half of it is gone after about ${halfLives(config)}. " +
                    "Big leg muscles take longest. A muscle shows as ready when 6 hard sets would be about " +
                    "90% recovered, which is 1.5 to 3 days for most muscles.",
            )
            Swatch(colors.success, "Ready: ${config.readyThreshold.roundToInt()}% or more")
            Swatch(colors.warning, "Recovering: ${config.fatiguedThreshold.roundToInt()} to ${config.readyThreshold.roundToInt() - 1}%")
            Swatch(colors.danger, "Fatigued: below ${config.fatiguedThreshold.roundToInt()}%")
            Swatch(MaterialTheme.colorScheme.surfaceVariant, "Gray: not trained in the last 2 weeks")

            Heading("Weekly sets")
            Body(
                "Counts hard sets in the last 7 days, with the same full and half sets. A set with " +
                    "${config.easySetRir.roundToInt()} or more reps in reserve is too easy to count.",
            )
            Body(
                "Studies link ${sets(target.start)} to ${sets(target.endInclusive)} hard sets per muscle per week " +
                    "with the most muscle growth. Fewer still help. More add little and are harder to recover from. " +
                    "The line under each bar marks that range.",
            )
            Swatch(MuscleTints.weekly(WeeklyVolumeStatus.Below, colors)!!, "Below ${sets(target.start)} sets")
            Swatch(colors.success, "${sets(target.start)} to ${sets(target.endInclusive)} sets: in range")
            Swatch(colors.violet, "Above ${sets(target.endInclusive)} sets")

            Heading("Keep in mind")
            Body(
                "This is an estimate. Sleep, food, stress and how used you are to the training matter too. " +
                    "If a muscle still feels sore or weak, give it more time.",
            )

            Heading("Sources")
            RecoverySources.all.forEach { source ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.Button, onClickLabel = "Open source") { uriHandler.openUri(source.url) }
                        .padding(vertical = 4.dp),
                ) {
                    Text(source.title, style = MaterialTheme.typography.bodyMedium, color = colors.primaryText)
                    Text(source.citation, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = EmberTheme.colors.textPrimary,
        modifier = Modifier.padding(top = 6.dp).semantics { heading() },
    )
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = EmberTheme.colors.textSecondary)
}

@Composable
private fun Swatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape).border(1.dp, EmberTheme.colors.border, CircleShape))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = EmberTheme.colors.textPrimary, modifier = Modifier.padding(start = 10.dp))
    }
}

private fun share(value: Double): String = if (value == 0.5) "half" else "${(value * 100).roundToInt()}% of"

/** "12 hours for forearms and abs, 15 for shoulders, ...", from the config's half-lives. */
private fun halfLives(config: RecoveryConfig): String {
    val groups = MuscleGroup.entries.groupBy { config.halfLifeFor(it).roundToInt() }.entries.sortedBy { it.key }
    return groups.mapIndexed { index, (hours, muscles) ->
        val names = muscles.map { it.label.lowercase() }
        val list = if (names.size == 1) names.single() else names.dropLast(1).joinToString(", ") + " and " + names.last()
        if (index == 0) "$hours hours for $list" else "$hours for $list"
    }.let { parts -> parts.dropLast(1).joinToString(", ") + " and " + parts.last() }
}
