package com.lukr99.workout.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.WorkoutShareCardRenderer
import com.lukr99.workout.ui.theme.EmberTheme

/** Shows the exact image a past workout will be shared as, with Cancel and Share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutSharePreviewSheet(
    preview: WorkoutShareCardRenderer.PreparedShare,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Share preview", style = MaterialTheme.typography.titleLarge)
            Text("This exact image will be shared.", color = EmberTheme.colors.textSecondary)
            Image(
                bitmap = preview.bitmap.asImageBitmap(),
                contentDescription = "Workout share image preview",
                modifier = Modifier.fillMaxWidth().aspectRatio(preview.bitmap.width.toFloat() / preview.bitmap.height)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = onShare) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                    Text(" Share")
                }
            }
        }
    }
}
