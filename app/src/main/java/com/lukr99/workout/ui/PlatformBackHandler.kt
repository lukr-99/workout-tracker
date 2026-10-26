package com.lukr99.workout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Runs [onBack] when the person presses the system Back button or makes the back gesture, while
 * [enabled]. The web edition maps it to the browser's back button.
 *
 * Each platform supplies this file; the web edition's is under web/.
 */
@Composable
fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
