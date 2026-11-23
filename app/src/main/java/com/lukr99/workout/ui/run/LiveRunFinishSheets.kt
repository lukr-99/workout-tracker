package com.lukr99.workout.ui.run

import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.RunText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.RunStats
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.Format

/* The two sheets at the end of a live run: confirm Finish, then the summary with any PRs. */

@Composable
internal fun FinishSummarySheet(
    summary: RunStats.RunSummary,
    units: UnitSystem,
    onDone: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(RunText.runSaved.text(), color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                RunMetric(RunText.distance.text(), Format.distance(summary.distanceMeters, units))
                RunMetric(RunText.time.text(), Pace.formatDuration(summary.movingSeconds))
                RunMetric(RunText.pace.text(), runPaceLabel(summary.avgPaceSecPerKm, units))
            }
            if (summary.newRecords.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.newRecords.forEach { pr ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏅", fontSize = 16.sp)
                            Text(
                                "  " + RunText.newRecord.text("label" to prLabel(pr)),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary).clickable(onClick = onDone)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(CommonText.done.text(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun prLabel(pr: RunStats.PrKind): String = when (pr) {
    RunStats.PrKind.Fastest1k -> RunText.prFastest1k.text()
    RunStats.PrKind.Fastest5k -> RunText.prFastest5k.text()
    RunStats.PrKind.Fastest10k -> RunText.prFastest10k.text()
    RunStats.PrKind.FastestHalf -> RunText.prFastestHalf.text()
    RunStats.PrKind.LongestRun -> RunText.prLongestRun.text()
    RunStats.PrKind.MostElevation -> RunText.prMostElevation.text()
    RunStats.PrKind.BestAvgPace -> RunText.prBestAvgPace.text()
}

@Composable
internal fun FinishSheet(
    state: LiveRunState,
    units: UnitSystem,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = false) {}
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(RunText.finishRunQuestion.text(), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                RunMetric(RunText.distance.text(), Format.distance(state.distanceMeters, units))
                RunMetric(RunText.time.text(), Pace.formatDuration(state.movingSeconds))
                RunMetric(RunText.pace.text(), runPaceLabel(state.avgPaceSecPerKm, units))
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary).clickable(onClick = onSave)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(RunText.saveRun.text(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(RunText.keepRunning.text(), Modifier.weight(1f), onCancel)
                SecondaryButton(RunText.discard.text(), Modifier.weight(1f), onDiscard)
            }
        }
    }
}

@Composable
private fun SecondaryButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
    }
}
