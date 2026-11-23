package com.lukr99.workout.ui.screens

import com.lukr99.workout.ui.i18n.TemplateEditorText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.ui.theme.EmberTheme

/** Lists a superset's exercises and lets you add the neighbour before or after, remove one, or ungroup. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupersetEditorSheet(
    groupId: Int,
    entries: List<WorkoutEntry>,
    onAddBefore: () -> Unit,
    onAddAfter: () -> Unit,
    onRemoveEntry: (String) -> Unit,
    onUngroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val positions = entries.indices.filter { entries[it].supersetGroup == groupId }
    val members = positions.map(entries::get)
    val canAddBefore = positions.firstOrNull()?.let { it > 0 && entries[it - 1].supersetGroup == null } == true
    val canAddAfter = positions.lastOrNull()?.let { it < entries.lastIndex && entries[it + 1].supersetGroup == null } == true
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(TemplateEditorText.editSuperset.text(), style = MaterialTheme.typography.titleLarge)
            Text(TemplateEditorText.supersetHint.text(), color = EmberTheme.colors.textSecondary)
            members.forEachIndexed { index, entry ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}. ${entry.exerciseSnapshotName}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { onRemoveEntry(entry.id) }) { Text(TemplateEditorText.remove.text()) }
                }
            }
            if (canAddBefore) TextButton(onClick = onAddBefore) {
                Text(TemplateEditorText.addBefore.text("name" to entries[positions.first() - 1].exerciseSnapshotName))
            }
            if (canAddAfter) TextButton(onClick = onAddAfter) {
                Text(TemplateEditorText.addAfter.text("name" to entries[positions.last() + 1].exerciseSnapshotName))
            }
            TextButton(onClick = onUngroup) { Text(TemplateEditorText.ungroupSuperset.text(), color = MaterialTheme.colorScheme.error) }
        }
    }
}
