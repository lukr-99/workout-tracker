package com.lukr99.workout.ui.settings

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
import com.lukr99.workout.ui.i18n.CommonText
import com.lukr99.workout.ui.i18n.SettingsText
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
            toast(if (found) SettingsText.locationSaved.now("name" to location.name) else SettingsText.locationNoFix.now())
        }
    }
    var pendingPermission by remember { mutableStateOf<Location?>(null) }
    val requestPermission = rememberLocationPermissionRequest { result ->
        val place = pendingPermission
        pendingPermission = null
        when {
            place == null -> Unit
            result == LocationPermissionResult.Precise -> capture(place)
            result == LocationPermissionResult.ApproximateOnly ->
                toast(SettingsText.locationNeedsPrecise.now())
            else -> toast(SettingsText.locationNeedsPermission.now())
        }
    }

    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Location?>(null) }
    val active = locations.forPicker()
    val archived = locations.filter(Location::isArchived).sortedBy { it.name.lowercase() }

    ButtonRow(
        label = SettingsText.addGym.text(),
        hint = SettingsText.addGymHint.text(),
        action = SettingsText.add.text(),
        first = true,
        onClick = { adding = true },
    )
    (active + archived).forEach { location ->
        SettingsRow(
            label = location.name,
            hint = if (location.isArchived) SettingsText.archivedHint.text() else kindLabel(location),
            onClick = { renaming = location },
        ) {
            Icon(location.kind.icon(), contentDescription = null, tint = EmberTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                if (CAN_SAVE_POSITIONS && !location.isArchived && location.kind != LocationKind.Outdoors) {
                    PillButton(
                        if (location.point == null) SettingsText.setPosition.text() else SettingsText.updatePosition.text(),
                        busy = settingPosition == location.id,
                        onClick = {
                            if (hasLocationPermission()) capture(location)
                            else {
                                pendingPermission = location
                                requestPermission()
                            }
                        },
                    )
                }
                PillButton(if (location.isArchived) SettingsText.restore.text() else SettingsText.archive.text(), onClick = { onArchive(location, !location.isArchived) })
            }
        }
    }

    if (adding) {
        TextEntryDialog(
            title = SettingsText.addGym.text(),
            initial = "",
            placeholder = SettingsText.gymPlaceholder.text(),
            confirmLabel = SettingsText.add.text(),
            onDismiss = { adding = false },
            onSave = onAddGym,
        )
    }
    renaming?.let { location ->
        TextEntryDialog(
            title = SettingsText.renameLocation.text(),
            initial = location.name,
            placeholder = location.name,
            confirmLabel = CommonText.save.text(),
            onDismiss = { renaming = null },
            onSave = { onRename(location, it) },
        )
    }
}

@Composable
private fun kindLabel(location: Location): String = when (location.kind) {
    LocationKind.Outdoors -> SettingsText.outdoorsKind.text()
    LocationKind.Home -> SettingsText.homeKind.text("position" to positionLabel(location)).trimEnd()
    LocationKind.Gym -> SettingsText.gymKind.text("position" to positionLabel(location)).trimEnd()
}

@Composable
private fun positionLabel(location: Location): String = when {
    !CAN_SAVE_POSITIONS -> ""
    location.point == null -> SettingsText.noPosition.text()
    else -> SettingsText.positionSaved.text()
}

