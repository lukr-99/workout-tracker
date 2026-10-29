package com.lukr99.workout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.recovery.MuscleGroup
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * A stylised front and back muscle map. Each region is tinted by [tintOf] for its [MuscleGroup], or
 * left neutral when that returns null. Schematic rather than anatomical: enough to read at a glance.
 */
@Composable
fun BodyHeatmap(tintOf: (MuscleGroup) -> Color?, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    // Blend toward the base a touch so a tint sits in the surface rather than glowing raw.
    val resolve: (MuscleGroup) -> Color = { group -> tintOf(group)?.let { lerp(base, it, 0.82f) } ?: base }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FigureColumn("Front", Modifier.weight(1f)) { drawFront(resolve, base) }
        FigureColumn("Back", Modifier.weight(1f)) { drawBack(resolve, base) }
    }
}

@Composable
private fun FigureColumn(label: String, modifier: Modifier, draw: DrawScope.() -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(0.52f),
        ) { draw() }
        Text(label, style = MaterialTheme.typography.labelSmall, color = EmberTheme.colors.textSecondary)
    }
}

// Virtual coordinate space for both figures.
private const val VW = 120f
private const val VH = 260f

private fun DrawScope.vRect(x0: Float, y0: Float, x1: Float, y1: Float, color: Color, radius: Float = 5f) {
    val sx = size.width / VW
    val sy = size.height / VH
    drawRoundRect(
        color = color,
        topLeft = Offset(x0 * sx, y0 * sy),
        size = Size((x1 - x0) * sx, (y1 - y0) * sy),
        cornerRadius = CornerRadius(radius),
    )
}

private fun DrawScope.vOval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    val sx = size.width / VW
    val sy = size.height / VH
    drawOval(
        color = color,
        topLeft = Offset((cx - rx) * sx, (cy - ry) * sy),
        size = Size(rx * 2 * sx, ry * 2 * sy),
    )
}

private fun DrawScope.drawFront(c: (MuscleGroup) -> Color, base: Color) {
    // Head + neck (neutral)
    vOval(60f, 20f, 13f, 14f, base)
    vRect(54f, 31f, 66f, 41f, base)
    // Delts
    vOval(38f, 50f, 12f, 12f, c(MuscleGroup.Shoulders))
    vOval(82f, 50f, 12f, 12f, c(MuscleGroup.Shoulders))
    // Chest
    vRect(43f, 44f, 59f, 64f, c(MuscleGroup.Chest))
    vRect(61f, 44f, 77f, 64f, c(MuscleGroup.Chest))
    // Biceps / upper arms
    vRect(26f, 52f, 38f, 86f, c(MuscleGroup.Biceps))
    vRect(82f, 52f, 94f, 86f, c(MuscleGroup.Biceps))
    // Forearms
    vRect(24f, 88f, 35f, 120f, c(MuscleGroup.Forearms))
    vRect(85f, 88f, 96f, 120f, c(MuscleGroup.Forearms))
    // Abs / core
    vRect(50f, 66f, 70f, 106f, c(MuscleGroup.Abs))
    // Quads
    vRect(46f, 118f, 59f, 186f, c(MuscleGroup.Quads))
    vRect(61f, 118f, 74f, 186f, c(MuscleGroup.Quads))
    // Calves (shins)
    vRect(47f, 192f, 58f, 240f, c(MuscleGroup.Calves))
    vRect(62f, 192f, 73f, 240f, c(MuscleGroup.Calves))
}

private fun DrawScope.drawBack(c: (MuscleGroup) -> Color, base: Color) {
    // Head + neck
    vOval(60f, 20f, 13f, 14f, base)
    vRect(54f, 31f, 66f, 41f, base)
    // Rear delts
    vOval(38f, 50f, 12f, 12f, c(MuscleGroup.Shoulders))
    vOval(82f, 50f, 12f, 12f, c(MuscleGroup.Shoulders))
    // Upper back / traps + lats
    vRect(44f, 44f, 76f, 72f, c(MuscleGroup.Back))
    vRect(46f, 72f, 74f, 104f, c(MuscleGroup.Back))
    // Triceps
    vRect(26f, 52f, 38f, 86f, c(MuscleGroup.Triceps))
    vRect(82f, 52f, 94f, 86f, c(MuscleGroup.Triceps))
    // Forearms
    vRect(24f, 88f, 35f, 120f, c(MuscleGroup.Forearms))
    vRect(85f, 88f, 96f, 120f, c(MuscleGroup.Forearms))
    // Glutes
    vRect(46f, 110f, 74f, 136f, c(MuscleGroup.Glutes))
    // Hamstrings
    vRect(46f, 138f, 59f, 186f, c(MuscleGroup.Hamstrings))
    vRect(61f, 138f, 74f, 186f, c(MuscleGroup.Hamstrings))
    // Calves
    vRect(47f, 192f, 58f, 240f, c(MuscleGroup.Calves))
    vRect(62f, 192f, 73f, 240f, c(MuscleGroup.Calves))
}
