package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.ui.theme.Accents
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * An exercise further down the workout, folded to one row so the one you are on stays in view:
 * an empty status ring, the name, its body part and what is planned. Tapping it opens the full card.
 */
@Composable
internal fun CompactEntryRow(entry: WorkoutEntry, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val planned = listOfNotNull(
        "Superset".takeIf { entry.supersetGroup != null },
        when {
            !entry.isStrength -> "Cardio"
            entry.strengthSets.size == 1 -> "1 set planned"
            else -> "${entry.strengthSets.size} sets planned"
        },
    ).joinToString(" · ")
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(onClickLabel = "Open ${entry.exerciseSnapshotName}", onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).border(1.5.dp, colors.border, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                entry.exerciseSnapshotName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val part = entry.exerciseSnapshotPrimaryBodyPart
                if (part.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).background(Accents.bodyPart(part), CircleShape))
                        Text(part, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    }
                }
                Text(planned, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
    }
}
