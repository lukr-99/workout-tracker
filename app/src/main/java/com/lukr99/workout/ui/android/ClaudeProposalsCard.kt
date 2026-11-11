package com.lukr99.workout.ui.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.claude.Proposal
import com.lukr99.workout.ui.settings.PillButton
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Home's card for Claude's proposals that wait for the owner, so they are seen without push. Each
 * shows its reason and plan with Reject and Accept; a third or later one waits in Settings, Claude.
 */
@Composable
fun ClaudeProposalsCard(
    proposals: List<Proposal>,
    busy: Boolean,
    onAccept: (Proposal) -> Unit,
    onReject: (Proposal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.border, shape).padding(16.dp)
            .testTag(ClaudeTags.PROPOSALS_CARD),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = colors.primaryText, modifier = Modifier.size(22.dp))
            Text(title(proposals), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        }
        proposals.take(SHOWN).forEach { proposal ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(proposal.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                if (proposal.reason.isNotBlank()) {
                    Text(proposal.reason, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                proposalDetails(proposal).takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Reject", busy = busy) { onReject(proposal) }
                    PillButton("Accept", busy = busy) { onAccept(proposal) }
                }
            }
        }
        if (proposals.size > SHOWN) {
            Text("${proposals.size - SHOWN} more in Settings, Claude.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

private const val SHOWN = 2

private fun title(proposals: List<Proposal>): String = when {
    proposals.size > 1 -> "Claude proposed ${proposals.size} things"
    proposals.single().isTemplate -> "Claude proposed a template"
    else -> "Claude proposed an exercise"
}
