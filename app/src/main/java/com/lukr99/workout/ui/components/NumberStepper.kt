package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.Numbers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The reusable "editable suggestion input" (02-design-system.md): −/+ with hold-to-repeat around a
 * tappable value that becomes an inline field. 44dp hit areas. Works for reps (step 1, 0 decimals)
 * and weight (step 2.5, 1 decimal).
 */
@Composable
fun NumberStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    step: Double = 1.0,
    decimals: Int = 0,
    minValue: Double = 0.0,
    maxValue: Double = 100_000.0,
    valueWidth: androidx.compose.ui.unit.Dp = 56.dp,
) {
    fun clamp(v: Double) = v.coerceIn(minValue, maxValue)
    fun format(v: Double): String =
        if (decimals == 0) v.toLong().toString()
        else {
            val r = Math.round(v * 10.0) / 10.0
            if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
        }

    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Rounded.Remove, "decrease") { onValueChange(clamp(value - step)) }
        Box(Modifier.width(valueWidth), contentAlignment = Alignment.Center) {
            if (editing) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        Numbers.copy(
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        ),
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (decimals == 0) KeyboardType.Number else KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        text.toDoubleOrNull()?.let { onValueChange(clamp(it)) }
                        editing = false
                    }),
                )
            } else {
                Text(
                    format(value),
                    style = Numbers.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                text = format(value)
                                editing = true
                            }
                        },
                )
            }
        }
        StepButton(Icons.Rounded.Add, "increase") { onValueChange(clamp(value + step)) }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onStep: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(44.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onStep()
                    val repeat = scope.launch {
                        delay(400)
                        while (isActive) {
                            onStep()
                            delay(80)
                        }
                    }
                    waitForUpOrCancellation()
                    repeat.cancel()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}
