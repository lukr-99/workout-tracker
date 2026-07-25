package com.lukr99.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.Positive
import com.lukr99.workout.ui.theme.TextMid
import com.lukr99.workout.ui.theme.Warning

/**
 * The set-logging workhorse (02-design-system.md): `badge · reps [stepper] × kg [stepper] · ✓`.
 * The done set dims and its ✓ turns `positive`; a PR set tints the badge. Tapping the badge opens
 * set options (type/warmup/remove) via [onOptions]. Previous-set hint shows under the steppers.
 */
@Composable
fun SetRow(
    index: Int,
    set: StrengthSet,
    units: UnitSystem,
    done: Boolean,
    onReps: (Int) -> Unit,
    onWeightKg: (Double) -> Unit,
    onToggleDone: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    previousHint: String? = null,
) {
    val bg by animateColorAsState(
        if (done) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        label = "setRowBg",
    )
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SetBadge(index = index, set = set, onClick = onOptions)

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NumberStepper(
                value = set.reps.toDouble(),
                onValueChange = { onReps(it.toInt()) },
                step = 1.0,
                decimals = 0,
                valueWidth = 40.dp,
            )
            Text("×", color = TextMid)
            NumberStepper(
                value = Format.toDisplay(set.weightKg, units),
                onValueChange = { onWeightKg(Format.toKg(it, units)) },
                step = 2.5,
                decimals = 1,
                valueWidth = 52.dp,
            )
        }

        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (done) Positive.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
                .clickable(onClick = onToggleDone),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = if (done) "mark set not done" else "mark set done",
                tint = if (done) Positive else TextMid,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    if (previousHint != null) {
        Text(
            previousHint,
            style = MaterialTheme.typography.labelSmall,
            color = TextMid,
            modifier = Modifier.padding(start = 48.dp, top = 1.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun SetBadge(index: Int, set: StrengthSet, onClick: () -> Unit) {
    val (label, tint) = when {
        set.isPr -> "PR" to Positive
        set.isWarmup || set.setType == SetType.Warmup -> "W" to Warning
        set.setType == SetType.Drop -> "D" to MaterialTheme.colorScheme.secondary
        set.setType == SetType.Failure -> "F" to MaterialTheme.colorScheme.error
        else -> "${index + 1}" to TextMid
    }
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Numbers.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = tint)
    }
}
