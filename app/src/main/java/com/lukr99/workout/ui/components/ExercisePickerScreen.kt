package com.lukr99.workout.ui.components

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * [ExercisePicker] in its own full-screen window. It used to be a bottom sheet, and scrolling the
 * long list kept fighting the sheet's drag to dismiss. Here the list owns every vertical drag, and
 * the X or the back gesture closes it through [onDismiss].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExercisePickerScreen(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
    onCreate: ((String) -> Unit)? = null,
    title: String = "Add exercise",
    subtitle: String? = null,
    recentIds: List<String> = emptyList(),
    hint: (Exercise) -> String? = { null },
    onPickMany: ((List<Exercise>, Boolean) -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val colors = EmberTheme.colors
        val view = LocalView.current
        SideEffect { matchSystemBarIcons(view, colors.isDark) }
        Box(
            // A dialog is its own window, so it needs testTagsAsResourceId again for its tags.
            Modifier.fillMaxSize()
                .background(colors.surface)
                .semantics { testTagsAsResourceId = true }
                .testTag(AppTags.EXERCISE_PICKER)
                .safeDrawingPadding()
                .padding(top = 8.dp),
        ) {
            ExercisePicker(
                exercises = exercises,
                onPick = onPick,
                onCreate = onCreate,
                title = title,
                subtitle = subtitle,
                recentIds = recentIds,
                hint = hint,
                onPickMany = onPickMany,
                onClose = onDismiss,
            )
        }
    }
}

/**
 * Stretches the dialog window over the system bars, so the picker gets real insets to pad by,
 * and makes the bar icons contrast with the surface. Without this the window started below the
 * status bar but ran under the gesture bar, which covered the Add button.
 */
private fun matchSystemBarIcons(view: View, dark: Boolean) {
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
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
    }
}
