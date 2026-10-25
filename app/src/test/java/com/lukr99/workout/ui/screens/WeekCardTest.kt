package com.lukr99.workout.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.Role
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.domain.DayActivity
import com.lukr99.workout.domain.TrainingDay
import com.lukr99.workout.domain.TrainingWeek
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.WorkoutTheme
import com.lukr99.workout.domain.plusDays
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tapping a day in Home's week strip opens its workout or run, or asks which one first. */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class WeekCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val monday = LocalDate(2026, 10, 12)
    private val push = DayActivity("push", isRun = false, name = "Push day", atUtc = 1L)
    private val run = DayActivity("run", isRun = true, name = "Easy 5k", atUtc = 2L)
    private val pull = DayActivity("pull", isRun = false, name = "Pull day", atUtc = 3L)

    private val opened = mutableListOf<DayActivity>()

    private fun show() {
        val days = (0..6).map { TrainingDay(monday.plusDays(it), isToday = it == 1) }.toMutableList()
        days[0] = days[0].copy(activities = listOf(push))
        days[1] = days[1].copy(activities = listOf(run, pull))
        compose.setContent {
            WorkoutTheme(dark = true) {
                WeekCard(TrainingWeek(days, workouts = 2, liftedKg = 0.0, runMeters = 0.0), UnitSystem.Metric) { opened += it }
            }
        }
    }

    @Test
    fun aDayWithOneWorkoutOpensItDirectly() {
        show()

        compose.onNodeWithTag(AppTags.homeDay(monday))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(clickLabel("Open workout, Monday 12 Oct"))
            .performClick()

        assertEquals(listOf(push), opened)
    }

    @Test
    fun aDayWithSeveralAsksWhichToOpen() {
        show()

        compose.onNodeWithTag(AppTags.homeDay(monday.plusDays(1)))
            .assert(clickLabel("Choose a workout or run, Tuesday 13 Oct"))
            .performClick()
        compose.waitForIdle()
        assertEquals(emptyList<DayActivity>(), opened)

        // The sheet may still be sliding in, so run its click action rather than tapping a point.
        compose.onNodeWithTag(AppTags.dayActivity("pull")).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(listOf(pull), opened)
        compose.onNodeWithTag(AppTags.DAY_SHEET).assertDoesNotExist()
    }

    @Test
    fun aRestDayDoesNothing() {
        show()

        compose.onNodeWithTag(AppTags.homeDay(monday.plusDays(2))).assertHasNoClickAction()
        compose.onNodeWithTag(AppTags.homeDay(monday)).assertHasClickAction()
    }

    private fun clickLabel(label: String) = SemanticsMatcher("click label is \"$label\"") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }
}
