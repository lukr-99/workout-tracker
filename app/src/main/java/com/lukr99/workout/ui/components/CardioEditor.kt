package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.ui.theme.TextMid

/**
 * Cardio duration / distance / calories editor (Phase 4 — cardio entries were read-only). Each field
 * is a labelled [ValueCell] opening the shared [NumberPadSheet]; distance is stored in km.
 */
@Composable
fun CardioEditor(
    cardio: CardioEntryData,
    onChange: (CardioEntryData) -> Unit,
    modifier: Modifier = Modifier,
) {
    // null = closed; 0 = minutes, 1 = distance, 2 = calories
    var editing by remember { mutableStateOf<Int?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardioLabel("MIN", Modifier.weight(1f))
            CardioLabel("KM", Modifier.weight(1f))
            CardioLabel("KCAL", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ValueCell(display = (cardio.durationSeconds / 60).toString(), modifier = Modifier.weight(1f)) { editing = 0 }
            ValueCell(display = cardio.distanceKm?.let { trim(it) } ?: "–", modifier = Modifier.weight(1f)) { editing = 1 }
            ValueCell(display = cardio.calories?.let { it.toInt().toString() } ?: "–", modifier = Modifier.weight(1f)) { editing = 2 }
        }
    }

    when (editing) {
        0 -> NumberPadSheet(
            title = "Duration (minutes)",
            initial = (cardio.durationSeconds / 60).toDouble(),
            quickStep = 1.0,
            onValue = { onChange(cardio.copy(durationSeconds = (it * 60).toInt())) },
            onDismiss = { editing = null },
        )
        1 -> NumberPadSheet(
            title = "Distance (km)",
            initial = cardio.distanceKm ?: 0.0,
            quickStep = 0.5,
            allowDecimal = true,
            unitLabel = "km",
            onValue = { onChange(cardio.copy(distanceKm = it.takeIf { v -> v > 0.0 })) },
            onDismiss = { editing = null },
        )
        2 -> NumberPadSheet(
            title = "Calories",
            initial = cardio.calories ?: 0.0,
            quickStep = 10.0,
            onValue = { onChange(cardio.copy(calories = it.takeIf { v -> v > 0.0 })) },
            onDismiss = { editing = null },
        )
        else -> Unit
    }
}

@Composable
private fun CardioLabel(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = TextMid,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun trim(v: Double): String {
    val r = Math.round(v * 100.0) / 100.0
    return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
}
