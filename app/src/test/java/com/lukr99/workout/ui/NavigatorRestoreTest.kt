package com.lukr99.workout.ui

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Turning the phone recreates the activity. The open screen and tab must come back, not Home.
 * Every [Route] is listed here, so a new route that the saver forgets fails this test.
 */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class NavigatorRestoreTest {
    @get:Rule
    val compose = createComposeRule()

    private val everyRoute = listOf(
        Route.LiveWorkout,
        Route.LiveRun,
        Route.RunDetail("run-1"),
        Route.RoutePlanner,
        Route.Settings,
        Route.TemplateEditor(null),
        Route.TemplateEditor("template-1"),
        Route.ExerciseEditor(null, initialName = "Bulgarian split squat"),
        Route.ExerciseEditor("exercise-1"),
        Route.WorkoutDetail("session-1"),
        Route.ProgressDetail("exercise-2"),
        Route.DataTransfer,
        Route.PrivacyPolicy,
    )

    @Test
    fun rotationKeepsTheOpenScreenAndTab() {
        val tester = StateRestorationTester(compose)
        var nav: Navigator? = null
        tester.setContent { nav = rememberSaveable(saver = Navigator.Saver) { Navigator() } }
        compose.runOnIdle {
            checkNotNull(nav).switch(Tab.PROGRESS)
            everyRoute.forEach(checkNotNull(nav)::push)
        }
        val before = nav

        tester.emulateSavedInstanceStateRestore()

        compose.runOnIdle {
            val restored = checkNotNull(nav)
            assertNotSame("the navigator is rebuilt from saved state", before, restored)
            assertEquals(Tab.PROGRESS, restored.tab.value)
            assertEquals(everyRoute, restored.stack.toList())
            assertEquals(Route.PrivacyPolicy, restored.top)
        }
    }

    @Test
    fun liveWorkoutStaysOpen() {
        val nav = Navigator().apply { push(Route.LiveWorkout) }
        val restored = roundTrip(nav)
        assertEquals(Tab.HOME, restored.tab.value)
        assertEquals(Route.LiveWorkout, restored.top)
    }

    @Test
    fun unknownSavedRoutesAndTabsAreDropped() {
        val restored = Navigator.Saver.restore(
            arrayListOf("NO_SUCH_TAB", arrayListOf("Gone"), arrayListOf("WorkoutDetail"), arrayListOf("Settings")),
        )
        val nav = checkNotNull(restored)
        assertEquals(Tab.HOME, nav.tab.value)
        assertEquals(listOf<Route>(Route.Settings), nav.stack.toList())
    }

    private fun roundTrip(nav: Navigator): Navigator {
        val saved = with(Navigator.Saver) { SaverScope { true }.save(nav) }
        return checkNotNull(Navigator.Saver.restore(checkNotNull(saved)))
    }
}
