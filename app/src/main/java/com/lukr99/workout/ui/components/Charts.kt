package com.lukr99.workout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.TextMid

data class ChartPoint(val label: String, val value: Double)

/**
 * Smooth spline area chart with a scrub handle (ring-set `MetricChart` lineage — 02-design-system.md).
 * Used for e1RM / volume / bodyweight trends. Drag or tap to reveal the value at a point.
 */
@Composable
fun ProgressChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 180.dp,
    valueFormat: (Double) -> String = { it.toString() },
) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            EmptyHint("No data yet")
        }
        return
    }
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }
    val minV = points.minOf { it.value }
    val maxV = points.maxOf { it.value }
    val span = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val gridColor = MaterialTheme.colorScheme.outline
    val bgColor = MaterialTheme.colorScheme.background

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                valueFormat(points[selected].value),
                style = Numbers.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "  ${points[selected].label}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMid,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    fun pick(x: Float) {
                        if (points.size < 2) { selected = 0; return }
                        val frac = (x / size.width).coerceIn(0f, 1f)
                        selected = Math.round(frac * (points.size - 1)).coerceIn(0, points.lastIndex)
                    }
                    detectTapGestures { pick(it.x) }
                    detectDragGestures { change, _ -> pick(change.position.x) }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val n = points.size
                val topPad = 6.dp.toPx()
                val plotH = size.height - 12.dp.toPx()
                fun px(i: Int) = if (n == 1) size.width / 2f else size.width * i / (n - 1)
                fun py(v: Double): Float {
                    val norm = ((v - minV) / span).toFloat().coerceIn(0f, 1f)
                    return topPad + (1f - norm) * plotH
                }

                val coords = points.mapIndexed { i, p -> Offset(px(i), py(p.value)) }

                // baseline grid
                drawLine(gridColor, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)

                val line = smoothPath(coords)
                val area = Path().apply {
                    addPath(line)
                    lineTo(coords.last().x, size.height)
                    lineTo(coords.first().x, size.height)
                    close()
                }
                drawPath(
                    area,
                    Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(line, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

                // scrub guide + dot
                val sel = coords[selected]
                drawLine(
                    gridColor,
                    Offset(sel.x, 0f),
                    Offset(sel.x, size.height),
                    1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
                drawCircle(lineColor, radius = 5.dp.toPx(), center = sel)
                drawCircle(bgColor, radius = 2.dp.toPx(), center = sel)
            }
        }
    }
}

data class BarPoint(val label: String, val value: Double)

/** Weekly volume / frequency bars with the current (last) bar highlighted, others dimmed. */
@Composable
fun VolumeBars(
    bars: List<BarPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 120.dp,
    valueFormat: (Double) -> String = { it.toString() },
) {
    if (bars.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) { EmptyHint("No data yet") }
        return
    }
    val maxV = bars.maxOf { it.value }.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { i, bar ->
            val current = i == bars.lastIndex
            val frac = (bar.value / maxV).toFloat().coerceIn(0f, 1f)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (current) {
                    Text(
                        valueFormat(bar.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((height - 22.dp) * frac.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (current) barColor else barColor.copy(alpha = 0.32f)),
                )
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMid,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Inline sparkline for list rows — a thin trend line, no axes. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (values.size < 2) {
        Box(modifier) {}
        return
    }
    val minV = values.min()
    val maxV = values.max()
    val span = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    Canvas(modifier) {
        val n = values.size
        val coords = values.mapIndexed { i, v ->
            Offset(
                size.width * i / (n - 1),
                size.height * (1f - ((v - minV) / span).toFloat()),
            )
        }
        drawPath(smoothPath(coords), color, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

/** Quadratic midpoint smoothing — a light spline that never overshoots the data. */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        val midX = (prev.x + cur.x) / 2f
        val midY = (prev.y + cur.y) / 2f
        path.quadraticBezierTo(prev.x, prev.y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)
    return path
}
