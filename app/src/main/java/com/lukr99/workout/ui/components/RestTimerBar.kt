package com.lukr99.workout.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.EmberTheme
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
    next: String? = null,
) {
    val colors = EmberTheme.colors
    val fraction by animateFloatAsState(
        targetValue = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f),
        label = "restRing",
    )
    // Last-3s pulse: the ring gently breathes as the countdown nears zero.
    val pulsing = remainingSeconds in 1..3
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "restPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 1.14f else 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(500),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "restPulseScale",
    )
    val ring = colors.primary
    val track = colors.surfaceRaised
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.size(50.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
            ) {
                val stroke = 5.dp.toPx()
                val d = Size(size.width - stroke, size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
                drawArc(track, -90f, 360f, false, topLeft, d, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(ring, -90f, -360f * fraction, false, topLeft, d, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(Format.clock(remainingSeconds), style = Numbers.copy(fontSize = 28.sp), color = colors.textPrimary)
            Text(
                if (next != null) "Rest · next is $next" else "Rest",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Pill("+15s", filled = false, onClick = onAdd15)
        Pill("Skip", filled = true, onClick = onSkip)
    }
}

@Composable
private fun Pill(text: String, filled: Boolean, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.height(44.dp).clip(shape)
            .background(if (filled) colors.primary else colors.surfaceRaised)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = if (filled) colors.onPrimary else colors.textPrimary)
    }
}
