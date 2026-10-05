package com.lukr99.workout.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The four peer tabs of the 5-item shell (the center Start action is not a tab). Run Mode (v2.1)
 * reshuffled the slots: `Runs` takes History's old slot and History moved into `Progress`.
 */
enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    RUNS("Runs", Icons.AutoMirrored.Rounded.DirectionsRun),
    PROGRESS("Progress", Icons.Rounded.BarChart),
    SETTINGS("Settings", Icons.Rounded.Settings),
}
