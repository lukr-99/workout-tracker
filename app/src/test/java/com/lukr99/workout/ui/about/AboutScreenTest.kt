package com.lukr99.workout.ui.about

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The About page: its links open the right addresses, the edition picks its lines, and
 * `recordRoborazziDebug` writes `src/test/screenshots/screen_about_*.png` in light and dark.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = Application::class)
class AboutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<String>()
    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) { opened += uri }
    }

    private fun show(edition: AboutEdition = AboutEdition.Android, dark: Boolean = true) {
        compose.setContent {
            WorkoutTheme(dark = dark) {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AboutScreen(version = "2.11.0", edition = edition, onOpenPrivacy = { opened += "privacy" }, onBack = {})
                    }
                }
            }
        }
    }

    @Test
    fun linkButtonsOpenTheAuthorsProfilesAndTheIssues() {
        show()
        compose.onNodeWithTag(AppTags.ABOUT_INSTAGRAM).performScrollTo().performClick()
        compose.onNodeWithTag(AppTags.ABOUT_GITHUB).performScrollTo().performClick()
        compose.onNodeWithTag(AppTags.ABOUT_ISSUES).performScrollTo().performClick()
        assertEquals(listOf(AuthorLinks.INSTAGRAM_URL, AuthorLinks.GITHUB_URL, AuthorLinks.ISSUES_URL), opened)
    }

    @Test
    fun aLinkThatCannotOpenLeavesThePageAsItIs() {
        compose.setContent {
            WorkoutTheme(dark = true) {
                CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) = throw IllegalStateException("no browser")
                }) {
                    AboutScreen(version = "2.11.0", edition = AboutEdition.Android, onOpenPrivacy = {}, onBack = {})
                }
            }
        }
        compose.onNodeWithTag(AppTags.ABOUT_INSTAGRAM).performScrollTo().performClick()
        compose.onNodeWithTag(AppTags.ABOUT).assertExists()
    }

    @Test
    fun showsTheVersionAndTheEditionsCredits() {
        show(AboutEdition.Web)
        compose.onNodeWithText("Version 2.11.0").assertExists()
        compose.onNodeWithText("Web edition").assertExists()
        compose.onNodeWithText("Maps and routes").assertDoesNotExist()
        compose.onNodeWithText("Privacy policy").performScrollTo().performClick()
        assertEquals(listOf("privacy"), opened)
    }

    // A tall screen, so the whole page fits in one picture.
    @Config(qualifiers = "+h2600dp")
    @Test fun screenshotDark() = capture("screen_about_dark", dark = true)

    @Config(qualifiers = "+h2600dp")
    @Test fun screenshotLight() = capture("screen_about_light", dark = false)

    private fun capture(name: String, dark: Boolean) {
        show(dark = dark)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
