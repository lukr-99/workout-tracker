package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.TextMid

/**
 * A flat rounded section surface used instead of boxing everything in Material cards
 * (02-design-system.md "surfaces over cards"). Optional title row with a trailing slot.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null || trailing != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (title != null) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
                trailing?.invoke()
            }
        }
        content()
    }
}

/** A muted "nothing here yet" placeholder line, used across empty states. */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = TextMid, modifier = modifier)
}
