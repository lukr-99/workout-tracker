package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * A quiet one-line offer under the location chip: this place has no saved position, and the fix
 * taken at workout start can be it. Shown once per place; either answer puts it away for good.
 */
@Composable
fun PositionOfferRow(placeName: String, onSave: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Save this spot as $placeName?",
            style = MaterialTheme.typography.bodySmall,
            color = EmberTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSave) { Text("Save") }
        TextButton(onClick = onDismiss) { Text("No") }
    }
}
