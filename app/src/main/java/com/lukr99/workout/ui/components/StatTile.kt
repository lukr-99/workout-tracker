package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.ui.theme.Numbers

/**
 * Label (caption, muted) over a big tabular number, with an optional delta chip. A flat raised
 * surface — no heavy card (02-design-system.md "surfaces over cards"). Numbers are the hero.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    /** A context line under the value, like "+3 this month". */
    delta: String? = null,
    /** Green when true, red when false, plain when null (for lines like "last 30 days"). */
    deltaPositive: Boolean? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(EmberTheme.colors.surface)
            .border(1.dp, EmberTheme.colors.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = EmberTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            Text(
                value,
                style = Numbers.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
                color = EmberTheme.colors.textPrimary,
                maxLines = 1,
            )
            if (unit != null) {
                Text(
                    " $unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmberTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (delta != null) {
            Text(
                delta,
                style = MaterialTheme.typography.labelSmall,
                color = when (deltaPositive) {
                    true -> EmberTheme.colors.success
                    false -> MaterialTheme.colorScheme.error
                    null -> EmberTheme.colors.textSecondary
                },
            )
        }
    }
}
