package com.lukr99.workout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Filter or choice chip. A selected chip is filled with the primary colour; an unselected one is an
 * outline. [dot] adds a small colour mark in front, used for body parts. [accent] fills a selected
 * chip with another colour instead of the primary.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = EmberTheme.colors.primary,
    dot: Color? = null,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (selected) accent else Color.Transparent)
            .border(BorderStroke(1.dp, if (selected) accent else colors.border), shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null && !selected) Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.onPrimary else colors.textPrimary,
        )
    }
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
