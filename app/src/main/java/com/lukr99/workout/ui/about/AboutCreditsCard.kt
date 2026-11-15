package com.lukr99.workout.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * What Ember is made from: one row per credit with a tinted mark, the credit line and a licence
 * badge. The marks use the app's own accent colours, so the list reads at a glance.
 */
@Composable
fun AboutCreditsCard(edition: AboutEdition, modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Credits and licences",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        AboutContent.credits(edition).forEachIndexed { index, credit ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            CreditRow(credit)
        }
    }
}

@Composable
private fun CreditRow(credit: AboutCredit) {
    val colors = EmberTheme.colors
    val (icon, tint) = mark(credit.kind)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(credit.label, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(credit.credit, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
            Text(
                credit.licence,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun mark(kind: AboutCreditKind): Pair<ImageVector, Color> {
    val colors = EmberTheme.colors
    return when (kind) {
        AboutCreditKind.Source -> Icons.Rounded.Code to colors.primaryText
        AboutCreditKind.Type -> Icons.Rounded.TextFields to colors.violet
        AboutCreditKind.Icons -> Icons.Rounded.Widgets to colors.teal
        AboutCreditKind.Libraries -> Icons.Rounded.Extension to colors.success
        AboutCreditKind.Pictures -> Icons.Rounded.Image to colors.warning
        AboutCreditKind.Maps -> Icons.Rounded.Map to colors.teal
    }
}
