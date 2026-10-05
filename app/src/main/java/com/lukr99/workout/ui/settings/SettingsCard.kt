package com.lukr99.workout.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * One Settings section as a card: title, a one-line description, then its rows. [jump] (0 to 1)
 * lights the whole card after a jump: tint, a 2 dp ring, a soft glow and the left edge bar.
 * [scroll] (0 to 1) shows only the edge bar and the title colour when you scroll into it.
 */
@Composable
fun SettingsCard(
    section: SettingsSection,
    jump: Float,
    scroll: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val spot = colors.primary
    val edge = maxOf(jump, scroll * 0.85f)
    val tint = lerp(colors.surface, spot, 0.11f * jump)
    Column(
        modifier
            .fillMaxWidth()
            .drawBehind {
                if (jump > 0f) {
                    // 4 dp soft outer glow at about 20% of the spot colour.
                    val glow = 4.dp.toPx() * jump
                    drawRoundRect(
                        spot.copy(alpha = 0.2f * jump),
                        topLeft = Offset(-glow, -glow),
                        size = Size(size.width + glow * 2, size.height + glow * 2),
                        cornerRadius = CornerRadius(18.dp.toPx() + glow),
                    )
                }
            }
            .clip(shape)
            .background(tint)
            .border(if (jump > 0f) 2.dp else 1.dp, if (jump > 0f) lerp(colors.border, spot.copy(alpha = 0.65f), jump) else colors.border, shape)
            .drawBehind {
                if (edge > 0f) {
                    // 3 dp bar on the left edge, inset 14 dp top and bottom, growing from the middle.
                    val inset = 14.dp.toPx()
                    val full = size.height - inset * 2
                    val h = full * edge
                    drawRect(spot, topLeft = Offset(0f, inset + (full - h) / 2), size = Size(3.dp.toPx(), h))
                }
            }
            .padding(16.dp),
    ) {
        Text(
            section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = lerp(colors.textPrimary, colors.primaryText, maxOf(jump, scroll)),
            modifier = Modifier.semantics { heading() },
        )
        Text(section.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
        content()
    }
}
