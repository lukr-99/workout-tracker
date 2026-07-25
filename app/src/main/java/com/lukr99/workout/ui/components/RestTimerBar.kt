package com.lukr99.workout.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.Numbers

/**
 * Sticky rest-timer surface shown above the nav during a live workout (02-design-system.md): a
 * draining countdown ring, the remaining clock, +15s and Skip. The tick lives in the ViewModel;
 * this only renders [remainingSeconds] of [totalSeconds].
 */
@Composable
fun RestTimerBar(
    remainingSeconds: Int,
    totalSeconds: Int,
    onAdd15: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f),
        label = "restRing",
    )
    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(40.dp)) {
                val stroke = 4.dp.toPx()
                val d = Size(size.width - stroke, size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
                drawArc(track, -90f, 360f, false, topLeft, d, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(ring, -90f, -360f * fraction, false, topLeft, d, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Format.clock(remainingSeconds),
                style = Numbers.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        TextButton(onClick = onAdd15) { Text("+15s", color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onSkip) { Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
