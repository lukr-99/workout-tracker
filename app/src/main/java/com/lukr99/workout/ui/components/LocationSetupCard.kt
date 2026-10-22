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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Home's one-time "Where do you train?" card after the update that added locations. It names the
 * main gym and can put every past workout there. "Not now" puts it away for good; Settings keeps
 * the locations.
 */
@Composable
fun LocationSetupCard(
    unknownWorkouts: Int,
    onSave: (name: String, assignPast: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(20.dp)
    var name by rememberSaveable { mutableStateOf("") }
    var assignPast by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.primarySoft)
            .border(1.dp, colors.primary.copy(alpha = 0.35f), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Place, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(22.dp))
            Text(
                "Where do you train?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            "Name your main gym. Machines differ between gyms, so Ember can keep their numbers apart.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Brno gym") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .toggleable(value = assignPast, role = Role.Checkbox, onValueChange = { assignPast = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = assignPast, onCheckedChange = null, modifier = Modifier.padding(end = 8.dp))
            Text(
                if (unknownWorkouts == 1) "My past workout was there" else "My $unknownWorkouts past workouts were there",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
        Text(
            "Otherwise they stay at Unknown location and still count everywhere. You can move them later in History.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardButton("Not now", filled = false, onClick = onDismiss)
            CardButton("Save", filled = true, enabled = name.isNotBlank()) { onSave(name.trim(), assignPast) }
        }
    }
}

@Composable
internal fun CardButton(label: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = EmberTheme.colors
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = when {
            !enabled -> colors.textTertiary
            filled -> colors.onPrimary
            else -> colors.primaryText
        },
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(if (filled && enabled) Modifier.background(colors.primary) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
