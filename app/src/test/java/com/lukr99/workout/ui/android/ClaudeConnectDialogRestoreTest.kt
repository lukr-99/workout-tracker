package com.lukr99.workout.ui.android

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Turning the phone recreates the activity. What the owner typed into the connect form must still
 * be there afterwards (CodePrint RULES.md, "Tests and automation"; docs/pitfalls.md).
 */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class ClaudeConnectDialogRestoreTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rotationKeepsWhatWasTyped() {
        val tester = StateRestorationTester(compose)
        var connected: Pair<String, String>? = null
        tester.setContent {
            WorkoutTheme(dark = true) {
                ClaudeConnectDialog(error = null, connecting = false, onConnect = { url, secret -> connected = url to secret }, onDismiss = {})
            }
        }
        compose.onNodeWithTag(ClaudeTags.SERVER_ADDRESS).performTextInput("workout-tracker.you.workers.dev")
        compose.onNodeWithTag(ClaudeTags.SETUP_SECRET).performTextInput("a-setup-secret-of-at-least-32-chars")

        tester.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag(ClaudeTags.SERVER_ADDRESS).assertTextContains("workout-tracker.you.workers.dev")
        compose.onNodeWithTag(ClaudeTags.CONNECT).assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals("workout-tracker.you.workers.dev" to "a-setup-secret-of-at-least-32-chars", connected)
        }
    }
}
