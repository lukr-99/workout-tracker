package com.lukr99.workout.data.document

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.MemorySettingsArchive
import com.lukr99.workout.data.RoomTransactionRunner
import com.lukr99.workout.data.RoomWorkoutRepository
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.data.run.RoomRunRepository
import com.lukr99.workout.data.transfer.DataStoreSettingsArchive
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.NoPhotoArchive
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import com.lukr99.workout.settings.ThemeMode
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/**
 * A backup made on the web restores on Android to the same [ExportBundle], and an Android backup
 * restored on the web exports again unchanged, runs included.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class WebToAndroidBackupTest {

    private val db: WorkoutDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), WorkoutDb::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(Executor { it.run() })
        .setTransactionExecutor(Executor { it.run() })
        .build()
    private val androidSettings = MemorySettingsArchive()
    private val android = DataTransferService(
        RoomWorkoutRepository(db.workoutDao(), RoomTransactionRunner(db)),
        RoomRunRepository(db.runDao()),
        settings = androidSettings,
        appVersion = "1.0",
    )

    private val webDatabase = DocumentDatabase(FakeDocumentStore().asStore())
    private val webRepo = DocumentWorkoutRepository(webDatabase)
    private val webRuns = DocumentRunRepository(webDatabase)
    private val webSettings = DocumentSettingsStore(webDatabase)
    private val web = DataTransferService(
        webRepo,
        webRuns,
        photos = NoPhotoArchive,
        settings = DataStoreSettingsArchive(webSettings),
        appVersion = "1.0",
    )

    @After
    fun close() = db.close()

    @Test
    fun aWebBackupRestoresOnAndroidAsTheSameBundle() = runBlocking {
        webRepo.ensureSeeded()
        fillWith(webRepo, webRuns)
        webSettings.setThemeMode(ThemeMode.Dark)
        val webBackup = web.exportJson().text

        restore(android, webBackup)
        val androidBackup = android.exportJson().text

        assertEquals(comparable(webBackup), comparable(androidBackup))
        assertEquals("Dark", androidSettings.current.themeMode)
    }

    @Test
    fun anAndroidBackupRestoresOnTheWebAndExportsUnchanged() = runBlocking {
        val repo = RoomWorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        repo.ensureSeeded()
        fillWith(repo, RoomRunRepository(db.runDao()))
        androidSettings.current = SettingsSnapshot(units = "Imperial", defaultRestSeconds = 90)
        val androidBackup = android.exportJson().text

        restore(web, androidBackup)
        val webBackup = web.exportJson().text

        assertEquals(comparable(androidBackup), comparable(webBackup))
    }

    private suspend fun restore(service: DataTransferService, json: String) {
        val preview = service.previewImport(json, "backup.json", ImportOptions(mode = RestoreMode.Replace))
        check(preview.canCommit) { preview.plan.issues.toString() }
        service.commitImport(preview)
    }

    private fun comparable(json: String): ExportBundle = JsonExporter.fromJson(json).copy(exportedAtUtc = "")

    private suspend fun fillWith(
        repo: com.lukr99.workout.data.WorkoutRepository,
        runs: com.lukr99.workout.data.run.RunRepository,
    ) {
        val gym = repo.saveLocation(Location(id = "gym", name = "Brno gym"))
        val fly = repo.saveExercise(Exercise(id = "fly", name = "Cable Fly", trackPerLocation = true, instructions = "Squeeze"))
        repo.saveTemplate(
            WorkoutTemplate(
                id = "t1", name = "Push",
                exercises = listOf(WorkoutTemplateExercise(id = "te1", exerciseId = fly.id, exerciseName = fly.name, targetSets = 3, repsMin = 8, repsMax = 12, supersetGroup = 1)),
            ),
        )
        repo.saveWorkoutSession(
            WorkoutSession(
                id = "s1", templateId = "t1", name = "Push", startedAtUtc = 1_700_000_000_123L, endedAtUtc = 1_700_003_600_456L,
                status = WorkoutSessionStatus.Completed, locationId = gym.id, bodyweightKg = 80.5, perceivedEffort = 8,
                entries = listOf(
                    WorkoutEntry(
                        id = "e1", exerciseId = fly.id, exerciseSnapshotName = fly.name, notes = "Seat 4",
                        strengthSets = listOf(
                            StrengthSet(id = "set1", reps = 10, weightKg = 22.5, rpe = 8.5, tags = setOf(SetTag.Warmup, SetTag.Drop), performedAtUtc = 1_700_000_100_000L),
                            StrengthSet(id = "set2", reps = 8, weightKg = 25.0),
                        ),
                    ),
                    WorkoutEntry(
                        id = "e2", exerciseSnapshotName = "Bike", entryType = ExerciseCategory.Cardio,
                        cardioData = CardioEntryData(durationSeconds = 600, distanceKm = 4.2, calories = 120.0),
                    ),
                ),
            ),
        )
        repo.saveLocationNote(fly.id, gym.id, "Double pulley")
        val routePoints = listOf(RoutePoint(0, 50.08, 14.42), RoutePoint(1, 50.09, 14.43))
        runs.saveRoute(Route(id = "route1", name = "Loop", createdAtUtc = 5, encodedPolyline = Polyline.encodeRoute(routePoints), points = routePoints))
        // Runs reach the web only from an Android backup, so their polyline already matches the trace.
        val trace = listOf(TracePoint(t = 0, lat = 50.08, lon = 14.42), TracePoint(t = 5_000, lat = 50.081, lon = 14.421, segmentStart = true))
        runs.saveRun(
            Run(
                id = "run1", startedAtUtc = 1_700_100_000_000L, durationSeconds = 600, distanceMeters = 2000.0, routeId = "route1",
                encodedPolyline = Polyline.encodeTrace(trace), trace = trace,
            ),
        )
    }
}
