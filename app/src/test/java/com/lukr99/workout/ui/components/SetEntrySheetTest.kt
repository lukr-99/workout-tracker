package com.lukr99.workout.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** OK on the set number pad keeps what you typed and goes back to the workout, without ticking the set. */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
class SetEntrySheetTest {
    @get:Rule
    val compose = createComposeRule()

    private var toggled = 0
    private var reps = 8

    private fun show() {
        compose.setContent {
            WorkoutTheme(dark = true) {
                SetRow(
                    index = 0,
                    total = 3,
                    set = StrengthSet(weightKg = 60.0, reps = reps),
                    units = UnitSystem.Metric,
                    done = false,
                    current = true,
                    exerciseName = "Back Squat",
                    previous = null,
                    before = null,
                    onReps = { reps = it },
                    onWeightKg = {},
                    onToggleDone = { toggled++ },
                    onOptions = {},
                )
            }
        }
    }

    @Test
    fun okClosesThePadWithoutMarkingTheSetDone() {
        show()

        compose.onNodeWithContentDescription("Reps, set 1", substring = true).performClick()
        compose.onNodeWithText("5").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("OK", substring = true).assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(0, toggled)
        assertEquals(5, reps)
        compose.onNodeWithText("OK", substring = true).assertDoesNotExist()
    }
}
