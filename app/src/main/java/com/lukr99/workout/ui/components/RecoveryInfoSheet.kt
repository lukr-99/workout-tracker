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
import com.lukr99.workout.ui.i18n.RecoveryText
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
                RecoveryText.infoTitle.text(),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            Heading(RecoveryText.headingRecovery.text())
            Body(
                if (config.secondaryMuscleContribution == 0.5) {
                    RecoveryText.infoFatigueHalf.text()
                } else {
                    RecoveryText.infoFatiguePercent.text("p" to (config.secondaryMuscleContribution * 100).roundToInt())
                },
            )
            Body(RecoveryText.infoEffort.text("max" to config.maxEffort, "min" to config.minEffort))
            Body(RecoveryText.infoFades.text("times" to halfLives(config)))
            Swatch(colors.success, RecoveryText.swatchReady.text("pct" to config.readyThreshold.roundToInt()))
            Swatch(
                colors.warning,
                RecoveryText.swatchRecovering.text(
                    "from" to config.fatiguedThreshold.roundToInt(),
                    "to" to config.readyThreshold.roundToInt() - 1,
                ),
            )
            Swatch(colors.danger, RecoveryText.swatchFatigued.text("pct" to config.fatiguedThreshold.roundToInt()))
            Swatch(MaterialTheme.colorScheme.surfaceVariant, RecoveryText.swatchGray.text())

            Heading(RecoveryText.headingWeekly.text())
            Body(RecoveryText.infoWeeklyCount.text("n" to config.easySetRir.roundToInt()))
            Body(RecoveryText.infoStudies.text("from" to sets(target.start), "to" to sets(target.endInclusive)))
            Swatch(MuscleTints.weekly(WeeklyVolumeStatus.Below, colors)!!, RecoveryText.swatchBelowSets.text("v" to sets(target.start)))
            Swatch(colors.success, RecoveryText.swatchRangeSets.text("from" to sets(target.start), "to" to sets(target.endInclusive)))
            Swatch(colors.violet, RecoveryText.swatchAboveSets.text("v" to sets(target.endInclusive)))

            Heading(RecoveryText.headingKeepInMind.text())
            Body(RecoveryText.infoEstimate.text())

            Heading(RecoveryText.headingSources.text())
            val openSource = RecoveryText.openSource.text()
            RecoverySources.all.forEach { source ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.Button, onClickLabel = openSource) { uriHandler.openUri(source.url) }
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

/** "12 hours for forearms and abs, 15 for shoulders, ...", from the config's half-lives. */
@Composable
private fun halfLives(config: RecoveryConfig): String {
    val and = RecoveryText.and.text()
    val groups = MuscleGroup.entries.groupBy { config.halfLifeFor(it).roundToInt() }.entries.sortedBy { it.key }
    return groups.mapIndexed { index, (hours, muscles) ->
        val names = muscles.map { RecoveryText.muscleName(it).text() }
        val list = if (names.size == 1) names.single() else names.dropLast(1).joinToString(", ") + " $and " + names.last()
        if (index == 0) {
            RecoveryText.firstHalfLife.text("h" to hours, "list" to list)
        } else {
            RecoveryText.nextHalfLife.text("h" to hours, "list" to list)
        }
    }.let { parts -> parts.dropLast(1).joinToString(", ") + " $and " + parts.last() }
}
