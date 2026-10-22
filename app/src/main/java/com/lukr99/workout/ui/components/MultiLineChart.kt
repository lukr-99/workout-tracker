package com.lukr99.workout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Several trend lines on one shared time axis, with a legend that doubles as the readout: the legend
 * row of each line shows that line's value nearest to where you tap or drag. The whole chart has one
 * spoken summary for screen readers, since a canvas has no parts to focus.
 */
@Composable
fun MultiLineChart(
    lines: List<ChartLine>,
    summary: String,
    valueFormat: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val all = lines.flatMap { it.points }
    if (all.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) { EmptyHint("No data yet") }
        return
    }
    val minT = all.minOf { it.dateMillis }
    val maxT = all.maxOf { it.dateMillis }
    val minV = all.minOf { it.value }
    val maxV = all.maxOf { it.value }
    val spanV = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    var selected by remember(lines) { mutableFloatStateOf(1f) }
    val selectedT = minT + ((maxT - minT) * selected).toLong()
    val gridColor = MaterialTheme.colorScheme.outline

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = summary }
                .pointerInput(lines) {
                    detectTapGestures { selected = (it.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(lines) {
                    detectDragGestures { change, _ -> selected = (change.position.x / size.width).coerceIn(0f, 1f) }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val topPad = 8.dp.toPx()
                val sidePad = 6.dp.toPx()
                val plotH = size.height - 16.dp.toPx()
                val plotW = size.width - 2 * sidePad
                fun px(t: Long) = if (maxT == minT) size.width / 2f else sidePad + plotW * ((t - minT).toFloat() / (maxT - minT))
                fun py(v: Double) = topPad + (1f - ((v - minV) / spanV).toFloat().coerceIn(0f, 1f)) * plotH

                drawLine(gridColor, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                lines.forEach { line ->
                    val coords = line.points.map { Offset(px(it.dateMillis), py(it.value)) }
                    coords.zipWithNext().forEach { (a, b) ->
                        drawLine(
                            line.color, a, b, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round,
                            pathEffect = if (line.dashed) dash else null,
                        )
                    }
                    coords.forEach { drawCircle(line.color, radius = 3.5.dp.toPx(), center = it) }
                }
                val x = px(selectedT)
                drawLine(
                    gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            lines.forEach { line ->
                val nearest = line.points.minByOrNull { kotlin.math.abs(it.dateMillis - selectedT) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(line.color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        line.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (nearest != null) {
                        Text(
                            Format.shortDate(nearest.dateMillis) + "  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmberTheme.colors.textSecondary,
                        )
                        Text(
                            valueFormat(nearest.value),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}
