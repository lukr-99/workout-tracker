package com.lukr99.workout.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.BarPoint
import com.lukr99.workout.ui.components.ChartPoint
import com.lukr99.workout.ui.components.EmptyHint
import com.lukr99.workout.ui.components.Format
import com.lukr99.workout.ui.components.ProgressChart
import com.lukr99.workout.ui.components.SectionCard
import com.lukr99.workout.ui.components.StatTile
import com.lukr99.workout.ui.components.VolumeBars
import com.lukr99.workout.ui.theme.TextMid
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The Running section of the Progress tab (R2): totals + streak, weekly distance bars, an average-pace
 * trend line, and personal records — all derived by [com.lukr99.workout.domain.run.RunStats] and
 * exposed through [RunViewModel.stats]. Reuses the strength chart/stat components.
 */
@Composable
fun RunningProgressSection(
    vm: RunViewModel,
    units: UnitSystem,
    modifier: Modifier = Modifier,
) {
    val stats by vm.stats.collectAsState()

    if (!stats.hasRuns) {
        EmptyHint("No runs yet. Record a run to see your distance, pace trend and PRs.", modifier)
        return
    }

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Totals.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Total distance", Format.distance(stats.totals.distanceMeters, units), Modifier.weight(1f))
            StatTile("Runs", "${stats.totals.runCount}", Modifier.weight(1f))
            StatTile("Streak", "${stats.streakWeeks}", unit = "wk", modifier = Modifier.weight(1f))
        }

        SectionCard(title = "Weekly distance") {
            VolumeBars(
                bars = stats.weekly.map { BarPoint(weekLabel(it.startUtc), distanceValue(it.distanceMeters, units)) },
                valueFormat = { "%.1f".format(it) },
            )
        }

        val trend = stats.paceTrend
        if (trend.size >= 2) {
            SectionCard(title = "Average pace") {
                ProgressChart(
                    points = trend.map { (whenUtc, secPerKm) ->
                        val p = if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm
                        ChartPoint(label = Format.date(whenUtc), value = p)
                    },
                    valueFormat = { "${Pace.formatPace(it)} /${if (units == UnitSystem.Imperial) "mi" else "km"}" },
                )
            }
        }

        SectionCard(title = "Personal records") {
            val r = stats.records
            PrRow("Fastest 1K", r.fastest1k?.let { paceText(it.paceSecPerKm, units) })
            PrRow("Fastest 5K", r.fastest5k?.let { paceText(it.paceSecPerKm, units) })
            PrRow("Fastest 10K", r.fastest10k?.let { paceText(it.paceSecPerKm, units) })
            PrRow("Fastest half", r.fastestHalf?.let { paceText(it.paceSecPerKm, units) })
            PrRow("Longest run", r.longestRun?.let { Format.distance(it.value, units) })
            PrRow("Best avg pace", r.bestAvgPace?.let { paceText(it.value, units) })
            PrRow("Most elevation", r.mostElevation?.let { "${it.value.toInt()} m" })
        }
    }
}

@Composable
private fun PrRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMid, modifier = Modifier.weight(1f))
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (value != null) MaterialTheme.colorScheme.onBackground else TextMid,
        )
    }
}

private fun paceText(secPerKm: Double, units: UnitSystem): String {
    val perUnit = if (units == UnitSystem.Imperial) Pace.paceSecPerMile(secPerKm) else secPerKm
    return "${Pace.formatPace(perUnit)} /${if (units == UnitSystem.Imperial) "mi" else "km"}"
}

private fun distanceValue(meters: Double, units: UnitSystem): Double =
    if (units == UnitSystem.Imperial) meters / Pace.METERS_PER_MILE else meters / Pace.METERS_PER_KM

private val weekFormatter = DateTimeFormatter.ofPattern("d/M")

private fun weekLabel(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(weekFormatter)
