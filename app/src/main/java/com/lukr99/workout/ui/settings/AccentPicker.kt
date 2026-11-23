package com.lukr99.workout.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukr99.workout.settings.AccentColor
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.AccentPalette
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * One round swatch per accent colour. The chosen one has a ring and a check mark, so it never
 * depends on colour alone. Each swatch shows its fill with the theme's own text colour on top.
 */
@Composable
fun AccentPicker(selected: AccentColor, onSelect: (AccentColor) -> Unit, modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    Row(
        modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccentColor.entries.forEach { accent ->
            val swatch = AccentPalette.swatch(accent)
            val isSelected = accent == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.textPrimary else colors.border, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(swatch.fill)
                    .selectable(isSelected, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .semantics { contentDescription = accent.name }
                    .testTag(AppTags.accent(accent)),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = swatch.onFill, modifier = Modifier.size(20.dp))
            }
        }
    }
}
