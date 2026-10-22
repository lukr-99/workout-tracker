package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * The non-blocking "Track Cable Fly separately per gym?" card over the live workout. [detail] says
 * why, like "59 kg here, 15 kg at Brno gym".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitPromptCard(
    exerciseName: String,
    detail: String,
    onTrack: () -> Unit,
    onNotNow: () -> Unit,
    onNever: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surfaceRaised)
            .border(1.dp, colors.primary.copy(alpha = 0.35f), shape).padding(14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Track $exerciseName separately per gym?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Text(
            "$detail. Per gym, last time, records and progress only count workouts at this gym.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        // Wraps on narrow phones instead of squeezing the labels.
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        ) {
            CardButton("Don't ask again", filled = false, onClick = onNever)
            CardButton("Not now", filled = false, onClick = onNotNow)
            CardButton("Track per gym", filled = true, onClick = onTrack)
        }
    }
}
