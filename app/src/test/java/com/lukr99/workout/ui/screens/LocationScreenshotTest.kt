package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.lukr99.workout.domain.Location
import com.lukr99.workout.ui.components.LocationChip
import com.lukr99.workout.ui.components.PositionOfferRow
import com.lukr99.workout.ui.components.LocationSetupCard
import com.lukr99.workout.ui.components.SplitPromptCard
import com.lukr99.workout.ui.components.TrackPerGymRow
import com.lukr99.workout.ui.settings.LocationsSettings
import com.lukr99.workout.ui.settings.SettingsCard
import com.lukr99.workout.ui.settings.SettingsSection
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The location pieces in light and dark (see ComponentScreenshotTest for how to record). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = android.app.Application::class)
class LocationScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun locationPiecesLight() = capture("location_pieces_light", dark = false) { Pieces() }

    @Test fun locationPiecesDark() = capture("location_pieces_dark", dark = true) { Pieces() }

    @Test fun locationSettingsDark() = capture("location_settings_dark", dark = true) {
        SettingsCard(SettingsSection.Locations, jump = 0f, scroll = 0f) {
            LocationsSettings(
                locations = listOf(Location(id = "brno", name = "Brno gym"), Location(id = "old", name = "Old gym", isArchived = true)) +
                    Location.builtIns,
                onAddGym = {},
                onRename = { _, _ -> },
                onArchive = { _, _ -> },
                hasLocationPermission = { true },
                onSetPosition = { _, _ -> },
            )
        }
    }

    @Composable
    private fun Pieces() = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocationChip("Brno gym", onClick = {})
        PositionOfferRow(placeName = "Brno gym", onSave = {}, onDismiss = {})
        LocationSetupCard(unknownWorkouts = 48, onSave = { _, _ -> }, onDismiss = {})
        SplitPromptCard("Cable Fly", "59 kg here, 15 kg at Brno gym", onTrack = {}, onNotNow = {}, onNever = {})
        TrackPerGymRow(checked = true, onChange = {})
    }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            WorkoutTheme(dark = dark) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
