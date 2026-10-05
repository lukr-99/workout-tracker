package com.lukr99.workout.ui.run.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.ui.theme.EmberTheme
import kotlin.math.cos
import kotlin.math.max

/**
 * The shape of a run or route as a small orange line on a raised tile, for lists. No map tiles, so
 * it draws offline and instantly. [encoded] is a Google polyline; [points] wins when it is given.
 */
@Composable
fun MiniRoute(encoded: String, modifier: Modifier = Modifier, points: List<Pair<Double, Double>> = emptyList()) {
    val colors = EmberTheme.colors
    val line = remember(encoded, points) { points.ifEmpty { runCatching { Polyline.decode(encoded) }.getOrDefault(emptyList()) } }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(colors.surfaceRaised)) {
        if (line.size < 2) return@Box
        Canvas(Modifier.matchParentSize()) {
            // An equirectangular projection is plenty for a thumbnail a few kilometres across.
            val midLat = Math.toRadians(line.sumOf { it.first } / line.size)
            val xs = line.map { it.second * cos(midLat) }
            val ys = line.map { -it.first }
            val minX = xs.min()
            val minY = ys.min()
            val span = max(xs.max() - minX, ys.max() - minY).takeIf { it > 0 } ?: return@Canvas
            val pad = 6.dp.toPx()
            val scale = (minOf(size.width, size.height) - pad * 2) / span
            val offsetX = (size.width - (xs.max() - minX) * scale) / 2
            val offsetY = (size.height - (ys.max() - minY) * scale) / 2
            val path = Path()
            line.indices.forEach { i ->
                val x = (offsetX + (xs[i] - minX) * scale).toFloat()
                val y = (offsetY + (ys[i] - minY) * scale).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, colors.primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
