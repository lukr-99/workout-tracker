package com.lukr99.workout.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationKind
import com.lukr99.workout.domain.forPicker
import com.lukr99.workout.domain.point
import com.lukr99.workout.ui.components.LocalToast
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
    hasLocationPermission: () -> Boolean,
    onSetPosition: (Location, (Boolean) -> Unit) -> Unit,
) {
    val toast = LocalToast.current
    var settingPosition by remember { mutableStateOf<String?>(null) }
    fun capture(location: Location) {
        settingPosition = location.id
        onSetPosition(location) { found ->
            settingPosition = null
            toast(if (found) "Saved the position of ${location.name}." else "No location fix. Try again outside or near a window.")
        }
    }
    var pendingPermission by remember { mutableStateOf<Location?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val place = pendingPermission
        pendingPermission = null
        if (place != null && result.values.any { it }) capture(place)
        else if (place != null) toast("Location permission is needed to save a position.")
    }

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
            hint = if (location.isArchived) "Archived. Its workouts keep it." else kindLabel(location),
            onClick = { renaming = location },
        ) {
            Icon(location.kind.icon(), contentDescription = null, tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                if (!location.isArchived && location.kind != LocationKind.Outdoors) {
                    PillButton(
                        if (location.point == null) "Set position" else "Update position",
                        busy = settingPosition == location.id,
                        onClick = {
                            if (hasLocationPermission()) capture(location)
                            else {
                                pendingPermission = location
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        },
                    )
                }
                PillButton(if (location.isArchived) "Restore" else "Archive", onClick = { onArchive(location, !location.isArchived) })
            }
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

private fun kindLabel(location: Location): String = when (location.kind) {
    LocationKind.Outdoors -> "Built in. Has no fixed position. Tap to rename."
    LocationKind.Home -> "Built in. ${positionLabel(location)}"
    LocationKind.Gym -> "Gym. ${positionLabel(location)}"
}

private fun positionLabel(location: Location): String =
    if (location.point == null) "No position yet, so it is not suggested by GPS." else "Position saved. Suggested when you are near."

