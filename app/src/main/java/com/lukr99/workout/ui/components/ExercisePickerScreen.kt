package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    val colors = EmberTheme.colors
    FullScreenDialog(onDismissRequest = onDismiss, darkBarIcons = !colors.isDark) {
        Box(
            // A dialog is its own window, so it needs testTagsAsResourceId again for its tags.
            Modifier.fillMaxSize()
                .background(colors.surface)
                .exposeTestTags()
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
