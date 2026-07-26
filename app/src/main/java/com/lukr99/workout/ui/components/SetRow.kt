package com.lukr99.workout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
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
 * The set-logging workhorse (02-design-system.md): `badge · REPS · × · KG · ✓`. Reworked in Phase 4
 * for touch: the value cells are big and tappable, opening a [NumberPadSheet] (quick ± + full
 * numpad); the columns are labelled by [SetColumnHeader] above the rows. The done set dims and its ✓
 * turns `positive`; a PR set tints the badge. Tapping the badge opens set options via [onOptions].
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
    // null = closed, false = editing reps, true = editing weight
    var editingWeight by remember { mutableStateOf<Boolean?>(null) }

    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetBadge(index = index, set = set, onClick = onOptions)

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ValueCell(
                display = formatReps(set.reps),
                modifier = Modifier.weight(1f),
            ) { editingWeight = false }
            Text("×", color = TextMid)
            ValueCell(
                display = formatWeight(Format.toDisplay(set.weightKg, units)),
                modifier = Modifier.weight(1.25f),
            ) { editingWeight = true }
        }

        val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
        val checkScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (done) 1f else 0.86f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
            ),
            label = "checkSpring",
        )
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (done) Positive.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
                .clickable {
                    if (!done) haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onToggleDone()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = if (done) "mark set not done" else "mark set done",
                tint = if (done) Positive else TextMid,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { scaleX = checkScale; scaleY = checkScale },
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

    when (editingWeight) {
        false -> NumberPadSheet(
            title = "Set ${index + 1} · Reps",
            initial = set.reps.toDouble(),
            quickStep = 1.0,
            onValue = { onReps(it.toInt()) },
            onDismiss = { editingWeight = null },
        )
        true -> NumberPadSheet(
            title = "Set ${index + 1} · Weight",
            initial = Format.toDisplay(set.weightKg, units),
            quickStep = 2.5,
            allowDecimal = true,
            unitLabel = Format.unitLabel(units),
            onValue = { onWeightKg(Format.toKg(it, units)) },
            onDismiss = { editingWeight = null },
        )
        null -> Unit
    }
}

/** REPS / KG labels aligned to the [SetRow] value columns. Render once above a set list. */
@Composable
fun SetColumnHeader(units: UnitSystem, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.width(32.dp)) // badge column
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColumnLabel("REPS", Modifier.weight(1f))
            Text("×", color = androidx.compose.ui.graphics.Color.Transparent)
            ColumnLabel(Format.unitLabel(units).uppercase(), Modifier.weight(1.25f))
        }
        Spacer(Modifier.width(38.dp)) // check column
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = TextMid,
            fontWeight = FontWeight.SemiBold,
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

private fun formatReps(reps: Int): String = reps.toString()

private fun formatWeight(v: Double): String {
    val r = Math.round(v * 10.0) / 10.0
    return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
}
