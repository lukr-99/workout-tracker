package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.recovery.RecoveryConfig
import com.lukr99.workout.domain.recovery.RecoveryEngine
import com.lukr99.workout.domain.recovery.RecoverySnapshot
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.components.MuscleMapMode
import com.lukr99.workout.ui.components.MuscleRecoveryCard
import com.lukr99.workout.ui.components.RecoveryInfoSheet
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Progress muscle map in both modes and its info sheet. `recordRoborazziDebug` writes the PNGs. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = android.app.Application::class)
class MuscleMapScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun recoveryDark() = capture("muscle_map_recovery_dark", dark = true) { MuscleRecoveryCard(snapshot) }

    @Test fun recoveryLight() = capture("muscle_map_recovery_light", dark = false) { MuscleRecoveryCard(snapshot) }

    @Test fun weeklyDark() = capture("muscle_map_weekly_dark", dark = true) {
        MuscleRecoveryCard(snapshot, initialMode = MuscleMapMode.WeeklySets)
    }

    @Test fun weeklyLight() = capture("muscle_map_weekly_light", dark = false) {
        MuscleRecoveryCard(snapshot, initialMode = MuscleMapMode.WeeklySets)
    }

    @Test fun infoSheetDark() {
        compose.setContent { WorkoutTheme(dark = true) { RecoveryInfoSheet(RecoveryConfig(), onDismiss = {}) } }
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/muscle_map_info_dark.png")
    }

    @Test fun switchAndInfoButtonWork() {
        compose.setContent { WorkoutTheme(dark = true) { MuscleRecoveryCard(snapshot) } }
        compose.onNodeWithTag(AppTags.muscleRow("Chest")).assertIsDisplayed()
        compose.onNodeWithTag(AppTags.muscleMapMode(MuscleMapMode.WeeklySets)).performClick()
        compose.onNodeWithTag(AppTags.muscleRow("Abs")).assertIsDisplayed()
        compose.onNodeWithText("of 11 in range", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(AppTags.MUSCLE_MAP_INFO).performClick()
        compose.onNodeWithText("How the muscle map works").assertIsDisplayed()
    }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            WorkoutTheme(dark = dark) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private companion object {
        const val Hour = 3_600_000L
        val bench = Exercise(id = "bench", primaryBodyPart = "Chest", secondaryBodyParts = listOf("Triceps", "Shoulders"))
        val row = Exercise(id = "row", primaryBodyPart = "Back", secondaryBodyParts = listOf("Biceps"))
        val squat = Exercise(id = "squat", primaryBodyPart = "Legs", secondaryBodyParts = listOf("Glutes"))
        val curl = Exercise(id = "curl", primaryBodyPart = "Biceps")
        val press = Exercise(id = "press", primaryBodyPart = "Shoulders", secondaryBodyParts = listOf("Triceps"))

        fun session(id: String, hoursAgo: Long, vararg work: Pair<Exercise, Int>) = WorkoutSession(
            id = id,
            startedAtUtc = Now - hoursAgo * Hour,
            completedDateUtc = Now - hoursAgo * Hour,
            status = WorkoutSessionStatus.Completed,
            entries = work.map { (exercise, sets) ->
                WorkoutEntry(
                    exerciseId = exercise.id,
                    exerciseSnapshotPrimaryBodyPart = exercise.primaryBodyPart,
                    strengthSets = List(sets) { StrengthSet(weightKg = 60.0, reps = 8, rir = 1.0) },
                )
            },
        )

        const val Now = 1_761_750_000_000L
        val snapshot: RecoverySnapshot = RecoveryEngine.calculate(
            sessions = listOf(
                session("push", 20, bench to 5, press to 4),
                session("pull", 50, row to 6, curl to 3),
                session("legs", 70, squat to 6),
                session("push2", 120, bench to 6, press to 4),
                session("pull2", 140, row to 8, curl to 4),
            ),
            exercises = listOf(bench, row, squat, curl, press),
            nowUtcMillis = Now,
        )
    }
}
