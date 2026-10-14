package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.DayActivity
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.EmberTheme

/** Lists a day's workouts and runs when Home's week strip has more than one on the tapped day. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DayActivitySheet(
    title: String,
    activities: List<DayActivity>,
    onOpen: (DayActivity) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = EmberTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            // A bottom sheet is its own window, so it needs testTagsAsResourceId again for its tags.
            Modifier.semantics { testTagsAsResourceId = true }.testTag(AppTags.DAY_SHEET)
                .fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            activities.forEach { activity ->
                Row(
                    Modifier.testTag(AppTags.dayActivity(activity.id))
                        .fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceRaised)
                        .clickable(role = Role.Button, onClickLabel = "Open ${activity.name}") { onOpen(activity) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                        Icon(
                            if (activity.isRun) Icons.AutoMirrored.Rounded.DirectionsRun else Icons.Rounded.FitnessCenter,
                            contentDescription = if (activity.isRun) "Run" else "Workout",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            activity.name.ifBlank { if (activity.isRun) "Run" else "Workout" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(Format.clockTime(activity.atUtc), style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
