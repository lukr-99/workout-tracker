package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * On Home when the automatic check found a verified new release: Update downloads and opens the
 * installer, Later puts it away until the next check. While it downloads, [status] replaces the
 * buttons.
 */
@Composable
fun UpdateCard(version: String, busy: Boolean, status: String, onUpdate: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.border, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = colors.primaryText, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Ember $version is ready", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    if (busy) status else "A signed update from GitHub. Your data stays as it is.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
        if (!busy) {
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Later",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onLater)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
                Text(
                    "Update",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.primary)
                        .clickable(role = Role.Button, onClick = onUpdate).padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}
