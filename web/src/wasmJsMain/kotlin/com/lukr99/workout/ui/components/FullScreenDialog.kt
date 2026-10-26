package com.lukr99.workout.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A dialog that fills the screen. [darkBarIcons] only matters on Android, where it colours the
 * system bar icons; a browser page has none. Each platform supplies this file.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    darkBarIcons: Boolean,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}
