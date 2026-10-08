package com.lukr99.workout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.TouchApp
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
import com.lukr99.workout.data.transfer.DataFormat
import com.lukr99.workout.data.transfer.ImportPlan
import com.lukr99.workout.data.transfer.ImportPreview
import com.lukr99.workout.data.transfer.ImportSummary
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.data.transfer.StoreCounts
import com.lukr99.workout.domain.BodyParts
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseOuting
import com.lukr99.workout.domain.PreviousEntryNote
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.ExercisePicker
import com.lukr99.workout.ui.components.QuickCreateSheet
import com.lukr99.workout.ui.components.SetEntrySheet
import com.lukr99.workout.ui.components.SetEntryState
import com.lukr99.workout.ui.components.SetOptionsSheet
import com.lukr99.workout.ui.settings.ButtonRow
import com.lukr99.workout.ui.settings.LinkRow
import com.lukr99.workout.ui.settings.SettingsCard
import com.lukr99.workout.ui.settings.SettingsRow
import com.lukr99.workout.ui.settings.SettingsSection
import com.lukr99.workout.ui.settings.ToggleRow
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

    private val catalog = listOf(
        Exercise(id = "incline", name = "Incline Dumbbell Press", primaryBodyPart = "Chest"),
        Exercise(id = "fly", name = "Cable Fly", primaryBodyPart = "Chest"),
        Exercise(id = "raise", name = "Lateral Raise", primaryBodyPart = "Shoulders"),
        Exercise(id = "squat", name = "Back Squat", primaryBodyPart = "Legs"),
        Exercise(id = "row", name = "Barbell Row", primaryBodyPart = "Back"),
    )

    @Test
    fun pickerDark() = capture("exercise_picker_dark", dark = true) {
        ExercisePicker(
            exercises = catalog,
            onPick = {},
            title = "Add exercises",
            subtitle = "Pick one or more. They go to the end of the workout.",
            recentIds = listOf("incline", "fly", "raise"),
            hint = { if (it.id == "incline") "Last 30 × 10" else null },
            onCreate = {},
            onPickMany = { _, _ -> },
        )
    }

    @Test
    fun setOptionsLight() = captureScreen("set_options_light", dark = false) {
        SetOptionsSheet(
            set = StrengthSet(reps = 8, weightKg = 82.5, rir = 1.0, rpe = 9.0, tags = setOf(SetTag.ToFailure), notes = "Left shoulder a bit tight."),
            onToggleTag = {},
            onRepsInReserve = {},
            onEditNote = {},
            onDuplicate = {},
            onRemove = {},
            onDismiss = {},
        )
    }

    @Test
    fun quickCreateDark() = captureScreen("quick_create_dark", dark = true) {
        QuickCreateSheet(initialName = "Landmine press", bodyParts = BodyParts.common.take(10), onCreate = {}, onDismiss = {})
    }

    @Test
    fun emptyWorkoutLight() = capture("empty_workout_start_light", dark = false) {
        EmptyWorkoutStart(
            recent = catalog,
            repeatable = listOf("Push day" to 1, "Pull day" to 3).map { (name, daysAgo) ->
                WorkoutSession(
                    name = name,
                    startedAtUtc = System.currentTimeMillis() - daysAgo * 86_400_000L,
                    entries = catalog.take(5).map { WorkoutEntry(exerciseId = it.id, exerciseSnapshotName = it.name) },
                )
            },
            templates = listOf("Push day", "Pull day", "Legs").map { WorkoutTemplate(name = it) },
            onAddExercises = {},
            onQuickAdd = {},
            onRepeat = {},
            onSwitch = {},
        )
    }

    @Test
    fun finishWithTemplateChangesDark() = captureScreen("finish_workout_dark", dark = true) {
        val template = WorkoutTemplate(
            name = "Push day",
            exercises = listOf(
                WorkoutTemplateExercise(exerciseId = "bench", exerciseName = "Bench press", targetSets = 3),
                WorkoutTemplateExercise(exerciseId = "dips", exerciseName = "Dips", sortOrder = 1),
            ),
        )
        val session = WorkoutSession(
            name = "Push day",
            startedAtUtc = 0L,
            entries = listOf(
                WorkoutEntry(exerciseId = "bench", exerciseSnapshotName = "Bench press", strengthSets = List(4) { StrengthSet(reps = 8, weightKg = 80.0, performedAtUtc = 1L, isPr = it == 3) }),
                WorkoutEntry(exerciseId = "fly", exerciseSnapshotName = "Cable fly", sortOrder = 1, strengthSets = List(3) { StrengthSet(reps = 12, weightKg = 15.0, performedAtUtc = 1L) }),
            ),
        )
        FinishWorkoutSheet(session, template, UnitSystem.Metric, volumeKg = 3_100.0, nowUtcMillis = 52 * 60_000L, onFinish = { _, _ -> }, onDismiss = {})
    }

    @Test fun orientationDark() = capture("orientation_dark", dark = true) { Orientation() }
    @Test fun orientationLight() = capture("orientation_light", dark = false) { Orientation() }

    /** The what's-new card, an empty state with an action, and a gesture hint. */
    @Composable
    private fun Orientation() = androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        com.lukr99.workout.ui.components.WhatsNewCard(com.lukr99.workout.domain.WhatsNew.notes.last(), onDismiss = {})
        com.lukr99.workout.ui.components.EmptyState(
            androidx.compose.material.icons.Icons.Rounded.ContentPaste,
            "No templates yet",
            "A template is a saved list of exercises to start a workout from, with a plan for each one if you like.",
            action = "New template",
        )
        com.lukr99.workout.ui.components.InlineHint(
            androidx.compose.material.icons.Icons.Rounded.TouchApp,
            "Tap a set's number for tags, effort, a note, or to remove it.",
        )
    }

    @Test fun settingsCardsDark() = capture("settings_cards_dark", dark = true) { SettingsCards() }
    @Test fun settingsCardsLight() = capture("settings_cards_light", dark = false) { SettingsCards() }

    @Composable
    private fun SettingsCards() = androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        SettingsCard(SettingsSection.Appearance, jump = 1f, scroll = 0f) {
            SettingsRow("Theme", "Follows the phone unless you pick one.", first = true)
            ToggleRow("Reduce motion", "Fewer animations.", checked = false) {}
        }
        SettingsCard(SettingsSection.YourData, jump = 0f, scroll = 0f) {
            ToggleRow("Automatic backup", "Daily to Documents/Ember. Last backup today 03:12.", checked = true, first = true) {}
            ButtonRow("Exercise catalog", "Download the open wger exercise database.", "Sync now", busy = true, busyLabel = "Syncing…") {}
            LinkRow("Import, export and delete", "Ember backups, Lyfta CSV, plain CSV.") {}
            LinkRow("All releases on GitHub", "Release notes and the manual download.", outside = true) {}
        }
    }

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
