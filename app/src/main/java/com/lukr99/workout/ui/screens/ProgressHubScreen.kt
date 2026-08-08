package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HistoryViewModel
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.run.RunViewModel
import com.lukr99.workout.ui.run.RunningProgressSection
import com.lukr99.workout.ui.theme.TextMid

/** The three panes of the look-back hub. */
private enum class HubPane { Progress, Running, History }

/**
 * The "look back" hub (Run Mode shell change): strength **Progress**, the new **Running** analytics,
 * and the workout **History**, switched by a segmented control now that History is no longer its own
 * tab. The Progress/History panes reuse the existing [ProgressScreen] / [HistoryScreen] unchanged;
 * Running renders [RunningProgressSection] (R2).
 */
@Composable
fun ProgressHubScreen(
    progressVm: ProgressViewModel,
    historyVm: HistoryViewModel,
    runVm: RunViewModel,
    units: UnitSystem,
    onOpenExercise: (String) -> Unit,
    onOpenSession: (String) -> Unit,
) {
    var pane by remember { mutableStateOf(HubPane.Progress) }

    Column(Modifier.fillMaxWidth()) {
        SegmentedToggle(
            pane = pane,
            onSelect = { pane = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        when (pane) {
            HubPane.Progress -> ProgressScreen(vm = progressVm, units = units, onOpenExercise = onOpenExercise)
            HubPane.Running -> RunningProgressSection(vm = runVm, units = units)
            HubPane.History -> HistoryScreen(vm = historyVm, units = units, onOpen = onOpenSession)
        }
    }
}

@Composable
private fun SegmentedToggle(pane: HubPane, onSelect: (HubPane) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Segment("Progress", selected = pane == HubPane.Progress, modifier = Modifier.weight(1f)) { onSelect(HubPane.Progress) }
        Segment("Running", selected = pane == HubPane.Running, modifier = Modifier.weight(1f)) { onSelect(HubPane.Running) }
        Segment("History", selected = pane == HubPane.History, modifier = Modifier.weight(1f)) { onSelect(HubPane.History) }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else TextMid,
            textAlign = TextAlign.Center,
        )
    }
}
