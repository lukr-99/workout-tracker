package com.lukr99.workout.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The bottom bar shows the Runs tab only where the platform has Run Mode (not in the web edition). */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class FloatingNavTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(showRuns: Boolean) {
        compose.setContent {
            WorkoutTheme {
                FloatingNav(
                    current = Tab.HOME,
                    resumeMode = false,
                    showRuns = showRuns,
                    onSelect = {},
                    onStart = {},
                    modifier = Modifier,
                )
            }
        }
    }

    @Test
    fun runsTabShowsWithRunMode() {
        show(showRuns = true)
        compose.onNodeWithTag(AppTags.nav(Tab.RUNS)).assertIsDisplayed()
    }

    @Test
    fun runsTabIsHiddenWithoutRunMode() {
        show(showRuns = false)
        compose.onNodeWithTag(AppTags.nav(Tab.RUNS)).assertDoesNotExist()
        compose.onNodeWithTag(AppTags.nav(Tab.PROGRESS)).assertIsDisplayed()
        compose.onNodeWithTag(AppTags.NAV_START).assertIsDisplayed()
    }
}
