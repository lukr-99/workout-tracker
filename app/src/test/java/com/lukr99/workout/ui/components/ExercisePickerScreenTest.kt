package com.lukr99.workout.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The exercise picker opens as its own full-screen window, not a sheet you can drag away. */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class ExercisePickerScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val squat = Exercise(id = "squat", name = "Back Squat", primaryBodyPart = "Legs")
    private val row = Exercise(id = "row", name = "Barbell Row", primaryBodyPart = "Back")

    private var dismissed = 0
    private val picked = mutableListOf<Exercise>()

    private fun show() {
        compose.setContent {
            WorkoutTheme(dark = true) {
                ExercisePickerScreen(
                    exercises = listOf(squat, row),
                    onDismiss = { dismissed++ },
                    onPick = { picked += it },
                    title = "Replace Bench Press",
                )
            }
        }
    }

    @Test
    fun theCloseButtonDismissesThePicker() {
        show()

        compose.onNodeWithTag(AppTags.EXERCISE_PICKER).assertIsDisplayed()
        compose.onNodeWithTag(AppTags.EXERCISE_PICKER_CLOSE).performClick()

        assertEquals(1, dismissed)
    }

    @Test
    fun tappingAnExercisePicksIt() {
        show()

        compose.onNodeWithText("Barbell Row").performClick()

        assertEquals(listOf(row), picked)
        assertEquals(0, dismissed)
    }
}
