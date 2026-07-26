package com.lukr99.workout.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.Positive
import com.lukr99.workout.ui.theme.TextMid

/**
 * The live PR celebration (Phase 4 motion item): a `positive`-glowing card whose estimated-1RM value
 * counts up on appearance. Wrap in `key(event.id) { PrBanner(...) }` so each new PR replays. The
 * haptic + auto-dismiss are driven by the caller.
 */
@Composable
fun PrBanner(
    exerciseName: String,
    headline: String,
    displayValue: Double,
    unitLabel: String,
    modifier: Modifier = Modifier,
) {
    val counted = remember1RmCountUp(displayValue)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Positive.copy(alpha = 0.16f))
            .border(1.dp, Positive.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.EmojiEvents, null, tint = Positive, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.titleMedium, color = Positive, fontWeight = FontWeight.Bold)
            Text(exerciseName, style = MaterialTheme.typography.labelMedium, color = TextMid)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                counted.toLong().toString(),
                style = Numbers.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                " $unitLabel e1RM",
                style = MaterialTheme.typography.labelSmall,
                color = TextMid,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun remember1RmCountUp(target: Double): Double {
    val anim = androidx.compose.runtime.remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.snapTo(0f)
        anim.animateTo(target.toFloat(), animationSpec = tween(650))
    }
    return anim.value.toDouble()
}
