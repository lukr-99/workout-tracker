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
import com.lukr99.workout.domain.recovery.RecoverySnapshot
import com.lukr99.workout.ui.theme.Danger
import com.lukr99.workout.ui.theme.Positive
import com.lukr99.workout.ui.theme.TextMid
import com.lukr99.workout.ui.theme.Warning

/**
 * The deferred Phase 2 component, built in Phase 4: a stylised front/back muscle map whose regions
 * are tinted by `insights.recovery` readiness (green = ready, amber → red = fatigued; neutral when
 * untrained). Schematic rather than anatomical — enough to read at a glance, Lyfta-style.
 */
@Composable
fun BodyHeatmap(recovery: RecoverySnapshot, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val resolve: (List<String>) -> Color = { names ->
        val vals = names.mapNotNull { recovery.forBodyPart(it)?.readiness }
        readinessColor(if (vals.isEmpty()) null else vals.average(), base)
    }

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
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMid)
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

private fun DrawScope.drawFront(colorOf: (List<String>) -> Color, base: Color) {
    fun c(vararg n: String) = colorOf(n.asList())
    // Head + neck (neutral)
    vOval(60f, 20f, 13f, 14f, base)
    vRect(54f, 31f, 66f, 41f, base)
    // Delts
    vOval(38f, 50f, 12f, 12f, c("Shoulders"))
    vOval(82f, 50f, 12f, 12f, c("Shoulders"))
    // Chest
    vRect(43f, 44f, 59f, 64f, c("Chest"))
    vRect(61f, 44f, 77f, 64f, c("Chest"))
    // Biceps / upper arms
    vRect(26f, 52f, 38f, 86f, c("Biceps", "Arms"))
    vRect(82f, 52f, 94f, 86f, c("Biceps", "Arms"))
    // Forearms
    vRect(24f, 88f, 35f, 120f, c("Forearms", "Arms"))
    vRect(85f, 88f, 96f, 120f, c("Forearms", "Arms"))
    // Abs / core
    vRect(50f, 66f, 70f, 106f, c("Abs", "Core"))
    // Quads
    vRect(46f, 118f, 59f, 186f, c("Legs", "Quadriceps", "Quads"))
    vRect(61f, 118f, 74f, 186f, c("Legs", "Quadriceps", "Quads"))
    // Calves (shins)
    vRect(47f, 192f, 58f, 240f, c("Calves"))
    vRect(62f, 192f, 73f, 240f, c("Calves"))
}

private fun DrawScope.drawBack(colorOf: (List<String>) -> Color, base: Color) {
    fun c(vararg n: String) = colorOf(n.asList())
    // Head + neck
    vOval(60f, 20f, 13f, 14f, base)
    vRect(54f, 31f, 66f, 41f, base)
    // Rear delts
    vOval(38f, 50f, 12f, 12f, c("Shoulders"))
    vOval(82f, 50f, 12f, 12f, c("Shoulders"))
    // Upper back / traps + lats
    vRect(44f, 44f, 76f, 72f, c("Back", "Traps"))
    vRect(46f, 72f, 74f, 104f, c("Back", "Lats"))
    // Triceps
    vRect(26f, 52f, 38f, 86f, c("Triceps", "Arms"))
    vRect(82f, 52f, 94f, 86f, c("Triceps", "Arms"))
    // Forearms
    vRect(24f, 88f, 35f, 120f, c("Forearms", "Arms"))
    vRect(85f, 88f, 96f, 120f, c("Forearms", "Arms"))
    // Glutes
    vRect(46f, 110f, 74f, 136f, c("Glutes"))
    // Hamstrings
    vRect(46f, 138f, 59f, 186f, c("Hamstrings", "Legs"))
    vRect(61f, 138f, 74f, 186f, c("Hamstrings", "Legs"))
    // Calves
    vRect(47f, 192f, 58f, 240f, c("Calves"))
    vRect(62f, 192f, 73f, 240f, c("Calves"))
}

/** Readiness → tint: 100 green, ~50 amber, 0 red; null (untrained) → neutral base. */
private fun readinessColor(readiness: Double?, base: Color): Color {
    if (readiness == null) return base
    val f = (readiness / 100.0).coerceIn(0.0, 1.0).toFloat()
    val hue = if (f < 0.5f) lerp(Danger, Warning, f * 2f) else lerp(Warning, Positive, (f - 0.5f) * 2f)
    // Blend toward the base a touch so it sits in the dark surface rather than glowing raw.
    return lerp(base, hue, 0.82f)
}
