package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
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
import com.lukr99.workout.ui.theme.Numbers
import com.lukr99.workout.ui.theme.Positive
import com.lukr99.workout.ui.theme.TextMid

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
    delta: String? = null,
    deltaPositive: Boolean = true,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMid,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            Text(
                value,
                style = Numbers.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            if (unit != null) {
                Text(
                    " $unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMid,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (delta != null) {
            Text(
                delta,
                style = MaterialTheme.typography.labelSmall,
                color = if (deltaPositive) Positive else MaterialTheme.colorScheme.error,
            )
        }
    }
}
