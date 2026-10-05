package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * A round icon button: an outlined surface circle, or a filled orange one for the main action on
 * a screen. [label] is what a screen reader says.
 */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: Dp = 44.dp,
) {
    val colors = EmberTheme.colors
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) colors.primary else colors.surface)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.border, CircleShape))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (filled) colors.onPrimary else colors.textPrimary, modifier = Modifier.size(size * 0.46f))
    }
}
