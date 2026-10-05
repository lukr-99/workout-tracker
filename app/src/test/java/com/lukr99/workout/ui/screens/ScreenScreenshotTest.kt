package com.lukr99.workout.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.lukr99.workout.data.RoomTransactionRunner
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.images.ExercisePhotoStore
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.HistoryViewModel
import com.lukr99.workout.ui.HomeViewModel
import com.lukr99.workout.ui.LibraryViewModel
import com.lukr99.workout.ui.LiveWorkoutViewModel
import com.lukr99.workout.ui.ProgressViewModel
import com.lukr99.workout.ui.theme.WorkoutTheme
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

/**
 * Whole screens with realistic data, in light and dark: the safety net for the redesign. Each
 * screen runs on its real view model over an in-memory database, so a screenshot shows what the
 * app shows. `recordRoborazziDebug` writes `src/test/screenshots/screen_*.png`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
// A plain Application: WorkoutApp.onCreate opens Room and seeds on a background thread, which leaks
// into the next test as "Illegal connection pointer" (docs/pitfalls.md).
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7, application = Application::class)
class ScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository

    @Before
    fun seed() = runBlocking {
        // Every query runs on the calling thread, so the screen has its data before the capture.
        val direct = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(direct)
            .setTransactionExecutor(direct)
            .build()
        repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        repo.ensureSeeded()
        ScreenFixtures.fill(repo)
    }

    // No @After close: leaving a screen saves its live draft, which runs after the test body. The
    // in-memory database goes away with the test process.

    @Test fun homeDark() = capture("screen_home_dark", dark = true) { Home() }
    @Test fun homeLight() = capture("screen_home_light", dark = false) { Home() }
    @Test fun libraryDark() = capture("screen_library_dark", dark = true) { Library() }
    @Test fun libraryLight() = capture("screen_library_light", dark = false) { Library() }
    @Test fun progressDark() = capture("screen_progress_dark", dark = true) { Progress() }
    @Test fun progressLight() = capture("screen_progress_light", dark = false) { Progress() }
    @Test fun historyDark() = capture("screen_history_dark", dark = true) { History() }
    @Test fun historyLight() = capture("screen_history_light", dark = false) { History() }
    @Test fun liveWorkoutDark() = capture("screen_live_workout_dark", dark = true) { Live() }
    @Test fun liveWorkoutLight() = capture("screen_live_workout_light", dark = false) { Live() }

    @Composable
    private fun Home() = HomeScreen(
        vm = HomeViewModel(repo),
        units = UnitSystem.Metric,
        onStart = {},
        onStartTemplate = {},
        onOpenLibrary = {},
        onOpenSession = {},
    )

    @Composable
    private fun Library() = LibraryScreen(
        vm = LibraryViewModel(repo, WorkoutDataService(repo), ExercisePhotoStore(context)),
        units = UnitSystem.Metric,
        onBack = {},
        onEditTemplate = {},
        onNewTemplate = {},
        onEditExercise = {},
        onNewExercise = {},
        onStartTemplate = {},
    )

    @Composable
    private fun Progress() = ProgressScreen(
        vm = ProgressViewModel(repo, WorkoutDataService(repo), WorkoutInsightsService(repo)),
        units = UnitSystem.Metric,
        onOpenExercise = {},
    )

    @Composable
    private fun History() = HistoryScreen(vm = HistoryViewModel(repo), units = UnitSystem.Metric, onOpen = {})

    @Composable
    private fun Live() {
        val vm = LiveWorkoutViewModel(repo, SettingsStore(context), WorkoutInsightsService(repo))
        vm.loadActiveIfAny()
        LiveWorkoutScreen(vm = vm, units = UnitSystem.Metric, onClose = {}, onCreateExercise = {}, onEditExercise = {})
    }

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            WorkoutTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
