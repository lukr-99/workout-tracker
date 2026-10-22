package com.lukr99.workout.ui.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationKind
import com.lukr99.workout.domain.forPicker
import com.lukr99.workout.ui.components.TextEntryDialog
import com.lukr99.workout.ui.components.icon
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * The Locations section: every place, tap to rename, archive to hide it from the picker (its
 * workouts keep it), and add a gym. Archived places sit at the end with Restore.
 */
@Composable
internal fun LocationsSettings(
    locations: List<Location>,
    onAddGym: (String) -> Unit,
    onRename: (Location, String) -> Unit,
    onArchive: (Location, Boolean) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Location?>(null) }
    val active = locations.forPicker()
    val archived = locations.filter(Location::isArchived).sortedBy { it.name.lowercase() }

    ButtonRow(
        label = "Add a gym",
        hint = "Pick the location when you start a workout. Exercises you track per gym keep their numbers apart.",
        action = "Add",
        first = true,
        onClick = { adding = true },
    )
    (active + archived).forEach { location ->
        SettingsRow(
            label = location.name,
            hint = if (location.isArchived) "Archived. Its workouts keep it." else kindLabel(location.kind),
            onClick = { renaming = location },
        ) {
            Icon(location.kind.icon(), contentDescription = null, tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
            PillButton(if (location.isArchived) "Restore" else "Archive", onClick = { onArchive(location, !location.isArchived) })
        }
    }

    if (adding) {
        TextEntryDialog(
            title = "Add a gym",
            initial = "",
            placeholder = "Brno gym",
            confirmLabel = "Add",
            onDismiss = { adding = false },
            onSave = onAddGym,
        )
    }
    renaming?.let { location ->
        TextEntryDialog(
            title = "Rename location",
            initial = location.name,
            placeholder = location.name,
            confirmLabel = "Save",
            onDismiss = { renaming = null },
            onSave = { onRename(location, it) },
        )
    }
}

private fun kindLabel(kind: LocationKind): String = when (kind) {
    LocationKind.Gym -> "Gym. Tap to rename."
    LocationKind.Home -> "Built in. Tap to rename."
    LocationKind.Outdoors -> "Built in. Tap to rename."
}
