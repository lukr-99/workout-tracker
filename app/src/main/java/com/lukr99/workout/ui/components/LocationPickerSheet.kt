package com.lukr99.workout.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationKind
import com.lukr99.workout.domain.forPicker
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * Picks where a workout happened: the active locations (gyms, then Home and Outdoors), "Unknown
 * location", and "Add a gym" for a new one, which is picked at once. [selectedId] gets a check
 * mark when [showSelection] is on.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LocationPickerSheet(
    title: String,
    locations: List<Location>,
    selectedId: String?,
    onPick: (String?) -> Unit,
    onAddGym: (String) -> Unit,
    onDismiss: () -> Unit,
    showSelection: Boolean = true,
) {
    var adding by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // A bottom sheet is its own window, so its tags need their own resource-id switch.
        Column(
            Modifier.fillMaxWidth()
                .semantics { testTagsAsResourceId = true }
                .testTag(AppTags.LOCATION_SHEET)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Exercises you track per gym show the numbers from here.",
                style = MaterialTheme.typography.bodySmall,
                color = EmberTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            locations.forPicker().forEach { location ->
                PickerRow(
                    icon = location.kind.icon(),
                    label = location.name,
                    selected = showSelection && location.id == selectedId,
                    modifier = Modifier.testTag(AppTags.location(location.id)),
                ) { onPick(location.id); onDismiss() }
            }
            PickerRow(Icons.Rounded.QuestionMark, Location.UNKNOWN_NAME, showSelection && selectedId == null) {
                onPick(null); onDismiss()
            }
            PickerRow(Icons.Rounded.Add, "Add a gym", selected = false, accent = true) { adding = true }
        }
    }
    if (adding) {
        TextEntryDialog(
            title = "Add a gym",
            initial = "",
            placeholder = "Brno gym",
            confirmLabel = "Add",
            onDismiss = { adding = false },
            onSave = { name -> onAddGym(name); onDismiss() },
        )
    }
}

/** The icon for a kind of place, shared by the picker and Settings. */
fun LocationKind.icon(): ImageVector = when (this) {
    LocationKind.Gym -> Icons.Rounded.FitnessCenter
    LocationKind.Home -> Icons.Rounded.Home
    LocationKind.Outdoors -> Icons.Rounded.Landscape
}

@Composable
private fun PickerRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = EmberTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (accent) colors.primaryText else colors.textSecondary, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected || accent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (accent) colors.primaryText else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = colors.primaryText, modifier = Modifier.size(22.dp))
    }
}
