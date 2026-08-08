package com.lukr99.workout.ui.run.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Split
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/**
 * Per-km/mi split table for the run detail screen: each split's pace as `m:ss` with a bar scaled to
 * the fastest split (longer = faster). The trailing partial split is labelled with its distance.
 */
@Composable
fun SplitTable(
    splits: List<Split>,
    units: UnitSystem,
    modifier: Modifier = Modifier,
) {
    if (splits.isEmpty()) return
    val fastest = splits.filter { it.paceSecPerKm > 0 }.minOfOrNull { it.paceSecPerKm } ?: return
    val unitLabel = if (units == UnitSystem.Imperial) "mi" else "km"

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        splits.forEach { split ->
            val displayPace = if (units == UnitSystem.Imperial) {
                Pace.paceSecPerMile(split.paceSecPerKm)
            } else {
                split.paceSecPerKm
            }
            val frac = if (split.paceSecPerKm > 0) (fastest / split.paceSecPerKm).toFloat() else 0f
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (split.isFull) "${split.index}" else "%.2f".format(split.distanceMeters / metersPer(units)),
                    style = Numbers,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(44.dp),
                )
                Box(
                    Modifier.weight(1f).height(16.dp).padding(end = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        Modifier.fillMaxWidth(frac.coerceIn(0.06f, 1f)).height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (split.isFull) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            ),
                    )
                }
                Text(
                    "${Pace.formatPace(displayPace)} /$unitLabel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (split.paceSecPerKm == fastest) MaterialTheme.colorScheme.primary else TextMid,
                )
            }
        }
    }
}

private fun metersPer(units: UnitSystem): Double =
    if (units == UnitSystem.Imperial) Pace.METERS_PER_MILE else Pace.METERS_PER_KM
