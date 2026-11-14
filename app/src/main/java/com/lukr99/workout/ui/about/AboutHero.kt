package com.lukr99.workout.ui.about

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.components.rememberReduceMotion
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * The top of the About page: the ember mark in a glowing ring, the name, the tagline, a line on what
 * Ember is and the version. Sparks drift up behind it; with reduce motion they hold still.
 */
@Composable
fun AboutHero(version: String, edition: AboutEdition, modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val drift = rememberSparkDrift()
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(colors.surfaceRaised, colors.surface)))
            .border(1.dp, colors.border, shape),
    ) {
        Sparks(drift, Modifier.matchParentSize())
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(84.dp)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.45f), Color.Transparent)),
                            radius = size.minDimension * 0.9f,
                        )
                    }
                    .clip(CircleShape)
                    .background(colors.primary)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(46.dp))
            }
            Text(
                "Ember",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, lineHeight = 52.sp),
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 16.dp).semantics { heading() },
            )
            Text(
                AboutContent.TAGLINE.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = colors.primaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                AboutContent.summary(edition),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                AboutContent.versionLabel(version),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.primarySoft)
                    .border(1.dp, colors.primary.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** 0 to 1, looping slowly; 0 for good with reduce motion. */
@Composable
internal fun rememberSparkDrift(): Float {
    if (rememberReduceMotion()) return 0f
    val transition = rememberInfiniteTransition(label = "about sparks")
    val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "drift")
    return drift
}

/** Small ember dots rising from the bottom and fading out near the top. Decoration only. */
@Composable
private fun Sparks(drift: Float, modifier: Modifier) {
    val spark = EmberTheme.colors.primary
    Canvas(modifier.clearAndSetSemantics { }) {
        SPARKS.forEach { (x, start, radius) ->
            val rise = (start + drift) % 1f
            val y = size.height * (1f - rise)
            val alpha = 0.55f * (1f - rise) * minOf(1f, rise * 6f)
            drawCircle(spark.copy(alpha = alpha), radius = radius.dp.toPx(), center = Offset(size.width * x, y))
        }
    }
}

/** Where each spark sits across the card, where it starts in its climb, and its size in dp. */
private val SPARKS = listOf(
    Triple(0.08f, 0.10f, 2.5f),
    Triple(0.17f, 0.55f, 1.6f),
    Triple(0.27f, 0.30f, 2.0f),
    Triple(0.74f, 0.80f, 1.8f),
    Triple(0.83f, 0.20f, 2.6f),
    Triple(0.92f, 0.62f, 1.5f),
    Triple(0.62f, 0.95f, 1.4f),
    Triple(0.38f, 0.72f, 1.3f),
)
