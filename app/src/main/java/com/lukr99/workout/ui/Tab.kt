package com.lukr99.workout.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The four tabs around the centre Start button, in bar order: Home and Library on the left, Runs
 * and Progress on the right. Settings is a screen opened from Home's gear, not a tab.
 */
enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    LIBRARY("Library", Icons.Rounded.FitnessCenter),
    RUNS("Runs", Icons.AutoMirrored.Rounded.DirectionsRun),
    PROGRESS("Progress", Icons.Rounded.BarChart),
}
