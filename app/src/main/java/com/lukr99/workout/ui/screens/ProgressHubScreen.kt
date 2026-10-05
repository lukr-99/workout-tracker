package com.lukr99.workout.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HistoryViewModel
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.components.ScreenHeader
import com.lukr99.workout.ui.components.SegmentedControl
import com.lukr99.workout.ui.run.RunViewModel
import com.lukr99.workout.ui.run.RunningProgressSection

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
        ScreenHeader("Progress", "How your training is going")
        Spacer(Modifier.height(12.dp))
        SegmentedControl(
            options = HubPane.entries,
            selected = pane,
            onSelect = { pane = it },
            label = { it.name },
            modifier = Modifier.padding(bottom = 12.dp),
        )
        when (pane) {
            HubPane.Progress -> ProgressScreen(vm = progressVm, units = units, onOpenExercise = onOpenExercise)
            HubPane.Running -> RunningProgressSection(vm = runVm, units = units)
            HubPane.History -> HistoryScreen(vm = historyVm, units = units, onOpen = onOpenSession)
        }
    }
}
