package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

/**
 * The big, mobile-first numeric editor (Phase 4 user feedback: the −/+ steppers were poor touch
 * input). Tapping a REPS / KG value opens this sheet: a large live value, quick ± chips (so the old
 * step behaviour survives), and a full numpad so typing is first-class. Ember minimal-dark taste.
 *
 * [onValue] fires live on every edit so the underlying [SetRow] updates behind the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberPadSheet(
    title: String,
    initial: Double,
    quickStep: Double,
    onValue: (Double) -> Unit,
    onDismiss: () -> Unit,
    allowDecimal: Boolean = false,
    unitLabel: String? = null,
    maxValue: Double = 100_000.0,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(formatBuffer(initial, allowDecimal)) }

    fun current(): Double = text.toDoubleOrNull() ?: 0.0
    fun push(value: Double) {
        val clamped = value.coerceIn(0.0, maxValue)
        text = formatBuffer(clamped, allowDecimal)
        onValue(clamped)
    }
    fun append(ch: Char) {
        val next = when {
            ch == '.' && (!allowDecimal || text.contains('.')) -> return
            text == "0" && ch != '.' -> ch.toString()
            else -> text + ch
        }
        if (next.length > 7) return
        text = next
        next.toDoubleOrNull()?.let { onValue(it.coerceIn(0.0, maxValue)) }
    }
    fun backspace() {
        text = text.dropLast(1).ifEmpty { "0" }
        onValue(current())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextMid)

            // Live value readout
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text,
                    style = Numbers.copy(fontSize = 48.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (unitLabel != null) {
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(
                        unitLabel,
                        style = Numbers.copy(fontSize = 18.sp),
                        color = TextMid,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            // Quick ± chips (keeps the old step behaviour, now with generous hit areas)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAdjustChip(Icons.Rounded.Remove, "−${trimStep(quickStep)}", Modifier.weight(1f)) {
                    push(current() - quickStep)
                }
                QuickAdjustChip(Icons.Rounded.Add, "+${trimStep(quickStep)}", Modifier.weight(1f)) {
                    push(current() + quickStep)
                }
            }

            // Numpad
            val rows = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3"),
                listOf(if (allowDecimal) "." else "", "0", "⌫"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(Modifier.weight(1f))
                                "⌫" -> KeyButton("⌫", Modifier.weight(1f), onClick = { backspace() })
                                else -> KeyButton(key, Modifier.weight(1f), onClick = { append(key[0]) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAdjustChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
        Text(label, style = Numbers.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun KeyButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "⌫") {
            Icon(
                Icons.AutoMirrored.Rounded.Backspace,
                "delete",
                tint = TextMid,
                modifier = Modifier.padding(2.dp),
            )
        } else {
            Text(
                label,
                style = Numbers.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

/** Backing-buffer format: integers show plain; decimals trim a trailing `.0`. */
private fun formatBuffer(v: Double, allowDecimal: Boolean): String =
    if (!allowDecimal) v.toLong().toString()
    else {
        val r = Math.round(v * 10.0) / 10.0
        if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
    }

private fun trimStep(step: Double): String =
    if (step % 1.0 == 0.0) step.toLong().toString() else step.toString()

/** Shared circle-badge for a set index/type, reused by [SetRow] and headers. */
@Composable
fun ValueCell(
    display: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (emphasised) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            display,
            style = Numbers.copy(fontSize = 19.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
