package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.lukr99.workout.ui.components.SetEntrySheet
import com.lukr99.workout.ui.components.SetEntryState
import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ImportPlan
import com.lukr99.workout.data.transfer.ImportPreview
import com.lukr99.workout.data.transfer.ImportSummary
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.data.transfer.StoreCounts
import com.lukr99.workout.domain.ExerciseOuting
import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshots of the components the redesign will touch, in light and dark. `testDebugUnitTest`
 * renders them as a smoke test; `recordRoborazziDebug` writes the PNGs to `src/test/screenshots/`
 * for a person or agent to look at, and `compareRoborazziDebug` writes diffs. Record and compare on
 * the same OS: rendering differs slightly between Windows and Linux.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A plain Application: WorkoutApp.onCreate opens Room and seeds on a background thread, which leaks
// into the next test as "Illegal connection pointer" (docs/pitfalls.md).
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = android.app.Application::class)
class ComponentScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun liveEntryCardWithNotesLight() = capture("live_entry_card_light", dark = false) { LiveCard() }

    @Test
    fun liveEntryCardWithNotesDark() = capture("live_entry_card_dark", dark = true) { LiveCard() }

    @Test
    fun replacePreviewDark() = capture("import_replace_preview_dark", dark = true) {
        ImportPreviewCard(preview = replacePreview, working = false, onMode = {}, onCommit = {}, onCancel = {})
    }

    @Test
    fun dangerZoneLight() = capture("data_danger_zone_light", dark = false) {
        DataDangerZone(counts = StoreCounts(exercises = 16), blocker = null, working = false, onErase = {})
    }

    @Test
    fun dangerZoneBlockedDark() = capture("data_danger_zone_blocked_dark", dark = true) {
        DataDangerZone(counts = null, blocker = "Finish or discard the live workout first.", working = false, onErase = {})
    }

    @Test
    fun setEntrySheetDark() = captureScreen("set_entry_sheet_dark", dark = true) { SetPad() }

    @Test
    fun setEntrySheetLight() = captureScreen("set_entry_sheet_light", dark = false) { SetPad() }

    @Composable
    private fun SetPad() = SetEntrySheet(
        title = "Barbell Bench Press",
        subtitle = "Set 4 of 4 · last time 80 × 6",
        initial = SetEntryState.of(weightDisplay = 82.5, reps = 8),
        unitLabel = "kg",
        weightStep = 2.5,
        copyLabel = "Set 3",
        copyFrom = 82.5 to 8,
        onChange = { _, _ -> },
        onDone = {},
        onDismiss = {},
    )

    @Test
    fun exerciseMenuDark() = captureScreen("exercise_menu_dark", dark = true) { Menu() }

    @Composable
    private fun Menu() = ExerciseMenuSheet(
        entry = WorkoutEntry(
            exerciseId = "bench",
            exerciseSnapshotName = "Barbell Bench Press",
            exerciseSnapshotPrimaryBodyPart = "Chest",
            strengthSets = listOf(StrengthSet(reps = 8, weightKg = 82.5, performedAtUtc = 1L)),
        ),
        units = UnitSystem.Metric,
        options = ExerciseMenuOptions(canSupersetWithPrevious = true, groupedWithPrevious = false, canMoveUp = true, canMoveDown = true, hasGuide = true),
        loadOutings = {
            listOf(
                ExerciseOuting(1_759_000_000_000, listOf(StrengthSet(reps = 8, weightKg = 80.0), StrengthSet(reps = 7, weightKg = 80.0)), 101.3),
                ExerciseOuting(1_758_600_000_000, listOf(StrengthSet(reps = 8, weightKg = 77.5), StrengthSet(reps = 8, weightKg = 77.5)), 98.2),
            )
        },
        onReplace = {}, onToggleSuperset = {}, onMoveUp = {}, onMoveDown = {}, onEditNote = {}, onShowGuide = {}, onRemove = {}, onDismiss = {},
    )

    /** Sheets and dialogs draw in their own window, so these capture the whole screen. */
    private fun captureScreen(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent { WorkoutTheme(dark = dark) { content() } }
        compose.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/$name.png")
    }

    @Composable
    private fun LiveCard() {
        val entry = WorkoutEntry(
            id = "e1",
            exerciseSnapshotName = "Barbell Bench Press",
            exerciseSnapshotPrimaryBodyPart = "Chest",
            notes = "Last rep slow, try 82.5 next time",
            startedAtUtc = 0L,
            strengthSets = listOf(
                StrengthSet(id = "s1", reps = 5, weightKg = 80.0, isPr = true, notes = "Spotter helped on rep 5"),
                StrengthSet(id = "s2", reps = 8, weightKg = 70.0, tags = setOf(SetTag.BackOff)),
            ),
        )
        LiveEntryCard(
            entry = entry,
            units = UnitSystem.Metric,
            doneIds = setOf("s1"),
            currentSetId = "s2",
            supersetPosition = null,
            supersetSize = 0,
            collapsed = false,
            nowUtcMillis = 120_000L,
            notes = EntryCardNotes(
                exerciseNote = "Bench 3, grip on the rings",
                previous = PreviousEntryNote("ex", "Felt heavy, sleep was short", atUtc = 1_759_000_000_000L),
                hasGuide = true,
            ),
            actions = noActions,
        )
    }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            WorkoutTheme(dark = dark) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private val replacePreview = ImportPreview(
        plan = ImportPlan(
            format = DataFormat.WorkoutJson,
            mode = RestoreMode.Replace,
            replaces = StoreCounts(exercises = 40, templates = 3, workouts = 120, runs = 30, routes = 4),
        ),
        summary = ImportSummary(
            parsedSessions = 118,
            setCount = 2_400,
            insertedExercises = 40,
            insertedRuns = 29,
            insertedRoutes = 4,
            photos = 6,
            metadata = mapOf("appVersion" to "2.6.0", "exportedAtUtc" to "2026-10-01T18:00:00Z"),
        ),
    )

    private val noActions = EntryCardActions(
        onOpenMenu = {}, onEditSuperset = {}, onToggleCollapsed = {}, onToggleWeightUnit = {},
        onStart = {}, onFinish = {}, onReopen = {}, onReps = { _, _ -> }, onWeight = { _, _ -> },
        onToggleDone = {}, onOptions = {}, onAddSet = {},
        onCardioChange = {}, onEditNote = {}, onShowGuide = {},
    )
}
