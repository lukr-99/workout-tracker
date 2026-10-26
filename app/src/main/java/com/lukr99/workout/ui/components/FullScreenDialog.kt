package com.lukr99.workout.ui.components

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * A dialog that fills the screen, including under the system bars, so the content can pad itself
 * by the real insets. [darkBarIcons] makes the status and navigation bar icons dark, for a light
 * surface. Each platform supplies this file; the web edition's is under web/.
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    darkBarIcons: Boolean,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        SideEffect { matchSystemBarIcons(view, darkBarIcons) }
        content()
    }
}

/**
 * Stretches the dialog window over the system bars, so the picker gets real insets to pad by,
 * and makes the bar icons contrast with the surface. Without this the window started below the
 * status bar but ran under the gesture bar, which covered the Add button.
 */
private fun matchSystemBarIcons(view: View, darkIcons: Boolean) {
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // A dialog window still fits itself between the bars unless its fit types are cleared.
        window.attributes = window.attributes.apply { fitInsetsTypes = 0 }
    }
    @Suppress("DEPRECATION")
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = darkIcons
        isAppearanceLightNavigationBars = darkIcons
    }
}
