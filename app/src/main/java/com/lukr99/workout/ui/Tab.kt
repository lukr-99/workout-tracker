package com.lukr99.workout.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.lukr99.workout.ui.i18n.AppText
import com.lukr99.workout.ui.i18n.Msg

/**
 * The four tabs around the centre Start button, in bar order: Home and Library on the left, Runs
 * and Progress on the right. Settings is a screen opened from Home's gear, not a tab.
 */
enum class Tab(val label: Msg, val icon: ImageVector) {
    HOME(AppText.tabHome, Icons.Rounded.Home),
    LIBRARY(AppText.tabLibrary, Icons.Rounded.FitnessCenter),
    RUNS(AppText.tabRuns, Icons.AutoMirrored.Rounded.DirectionsRun),
    PROGRESS(AppText.tabProgress, Icons.Rounded.BarChart),
}
