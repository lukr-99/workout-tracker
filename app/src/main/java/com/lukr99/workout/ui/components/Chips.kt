package com.lukr99.workout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Filter/choice pill (02-design-system.md). Selected chips fill with `primary @ 16%` and an ember
 * label; unselected are hairline outlines. Full-radius by design.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val bg = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent
    val border = if (selected) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** A small static tag (e.g. body-part), tinted by [accent]. Non-interactive. */
@Composable
fun Tag(label: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** A horizontally-scrolling row of filter chips is common; this lays them out with the standard gap. */
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
