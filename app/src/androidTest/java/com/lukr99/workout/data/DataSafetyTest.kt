package com.lukr99.workout.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.export.ExercisePhoto
import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.data.run.RunDao
import com.lukr99.workout.data.run.RunEntity
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.PhotoArchive
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.data.transfer.SettingsArchive
import com.lukr99.workout.data.transfer.UserDataEraser
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CodePrint data safety: a backup restores the whole store, a failed restore changes nothing, and
 * "Delete all data" returns the app to a fresh install. Runs against a real in-memory Room database
 * with in-memory fakes for photo files and settings.
 */
@RunWith(AndroidJUnit4::class)
class DataSafetyTest {

    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository
    private lateinit var runRepo: RunRepository
    private val photos = FakePhotoArchive()
    private val settings = FakeSettingsArchive()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java).allowMainThreadQueries().build()
        repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
        runRepo = RunRepository(db.runDao())
    }

    @After
    fun teardown() = db.close()

    private fun service(runs: RunRepository = runRepo) = DataTransferService(
        repository = repo,
        runRepository = runs,
        photos = photos,
        settings = settings,
        appVersion = "2.6.0-dev",
    )

    @Test
    fun replaceRestore_putsBackTheWholeStoreExactly() = runTest {
        val bench = repo.saveExercise(Exercise(name = "Bench", localImagePath = "/photos/bench.jpg"))
        photos.files["/photos/bench.jpg"] = byteArrayOf(1, 2, 3)
        repo.saveWorkoutSession(completed("kept", bench.id))
        runRepo.saveRoute(route("loop"))
        runRepo.saveRun(run("r1"))
        settings.current = SettingsSnapshot(themeMode = "Dark", units = "Imperial", defaultRestSeconds = 150)

        val backup = service().exportJson().text
        val bundle = JsonExporter.fromJson(backup)
        assertEquals("2.6.0-dev", bundle.appVersion)
        assertEquals(1, bundle.photos.size)

        // The phone changes after the backup: new data, other settings, the photo file gone.
        repo.saveExercise(Exercise(name = "Extra"))
        repo.saveWorkoutSession(completed("extra", bench.id))
        settings.current = SettingsSnapshot()
        photos.files.clear()

        val preview = service().previewImport(backup, "backup.json", ImportOptions(mode = RestoreMode.Replace))
        assertTrue(preview.canCommit)
        assertEquals(2, preview.plan.replaces?.workouts)
        val result = service().commitImport(preview)

        assertEquals(RestoreMode.Replace, result.mode)
        assertEquals(listOf("kept"), repo.getSessions().map { it.id })
        val exercises = repo.getExercises(ExerciseFilter(includeArchived = true))
        assertEquals(listOf(bench.id), exercises.map { it.id })
        val restoredPath = assertNotNullAnd(exercises.single().localImagePath)
        assertArrayEquals(byteArrayOf(1, 2, 3), photos.files[restoredPath])
        assertEquals(setOf(restoredPath), photos.files.keys)
        assertEquals(3, runRepo.getRun("r1")?.trace?.size)
        assertEquals(2, runRepo.getRoute("loop")?.points?.size)
        assertEquals("Dark", settings.current.themeMode)
        assertTrue(result.restoredSettings)
        assertEquals(1, result.restoredPhotos)
    }

    @Test
    fun failedRestore_leavesTheStoreAndPhotoFilesUntouched() = runTest {
        repo.saveWorkoutSession(completed("kept", "none"))
        val brokenRuns = RunRepository(object : RunDao by db.runDao() {
            override suspend fun upsertRun(run: RunEntity) = error("disk full")
        })
        val bundle = ExportBundle(
            exercises = listOf(Exercise(id = "ex1", name = "Squat")),
            sessions = listOf(completed("incoming", "ex1")),
            runs = listOf(run("r1")),
            photos = listOf(ExercisePhoto("ex1", dataBase64 = Base64.getEncoder().encodeToString(byteArrayOf(9)))),
        )
        val preview = service(brokenRuns).previewImport(JsonExporter.toJson(bundle), "backup.json")

        try {
            service(brokenRuns).commitImport(preview)
            fail("The restore should have failed")
        } catch (expected: IllegalStateException) {
            assertEquals("disk full", expected.message)
        }

        assertEquals(listOf("kept"), repo.getSessions().map { it.id })
        assertTrue(repo.getExercises(ExerciseFilter(includeArchived = true)).none { it.id == "ex1" })
        assertTrue("staged photos are deleted again", photos.files.isEmpty())
    }

    @Test
    fun mergeRestore_clearsAPhotoPathThatOnlyExistedOnAnotherPhone() = runTest {
        val bundle = ExportBundle(exercises = listOf(Exercise(id = "ex1", name = "Squat", localImagePath = "/other/squat.jpg")))
        val preview = service().previewImport(JsonExporter.toJson(bundle), "backup.json")
        service().commitImport(preview)

        assertNull(repo.getExercise("ex1")?.localImagePath)
    }

    @Test
    fun eraseAll_returnsToAFreshInstall() = runTest {
        repo.saveExercise(Exercise(name = "Custom", localImagePath = "/photos/custom.jpg"))
        photos.files["/photos/custom.jpg"] = byteArrayOf(1)
        repo.saveWorkoutSession(completed("done", "none"))
        runRepo.saveRun(run("r1"))
        runRepo.saveRoute(route("loop"))
        settings.current = SettingsSnapshot(units = "Imperial")
        var backupStopped = false

        eraser(stopBackup = { backupStopped = true }).eraseAll()

        val counts = service().currentCounts()
        assertEquals(0, counts.workouts)
        assertEquals(0, counts.runs)
        assertEquals(0, counts.routes)
        assertEquals(16, counts.exercises) // the starter catalog is back
        assertTrue(photos.files.isEmpty())
        assertEquals(SettingsSnapshot(), settings.current)
        assertTrue(backupStopped)
    }

    @Test
    fun eraseAll_refusesWhileAWorkoutIsLive() = runTest {
        repo.saveWorkoutSession(WorkoutSession(id = "live", status = WorkoutSessionStatus.Active))
        val eraser = eraser()

        assertNotNull(eraser.blocker())
        try {
            eraser.eraseAll()
            fail("Erasing during a live workout should be refused")
        } catch (expected: IllegalStateException) {
            assertEquals(listOf("live"), repo.getSessions().map { it.id })
        }
    }

    private fun eraser(stopBackup: suspend () -> Unit = {}) = UserDataEraser(
        repository = repo,
        runRepository = runRepo,
        photos = photos,
        settings = settings,
        stopAutomaticBackup = stopBackup,
        isRunRecording = { false },
    )

    private fun completed(id: String, exerciseId: String) = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = 1_000L,
        endedAtUtc = 2_000L,
        completedDateUtc = 2_000L,
        status = WorkoutSessionStatus.Completed,
        entries = listOf(
            WorkoutEntry(
                id = "$id-e",
                workoutSessionId = id,
                exerciseId = exerciseId,
                exerciseSnapshotName = "Lift",
                strengthSets = listOf(StrengthSet(id = "$id-s", reps = 5, weightKg = 100.0)),
            ),
        ),
    )

    private fun run(id: String) = Run(
        id = id,
        startedAtUtc = 1_000L,
        durationSeconds = 600,
        distanceMeters = 2_000.0,
        trace = listOf(
            TracePoint(t = 0, lat = 50.0, lon = 14.0),
            TracePoint(t = 1_000, lat = 50.001, lon = 14.0),
            TracePoint(t = 2_000, lat = 50.002, lon = 14.0),
        ),
    )

    private fun route(id: String) = Route(
        id = id,
        name = id,
        createdAtUtc = 1_000L,
        points = listOf(RoutePoint(seq = 0, lat = 50.0, lon = 14.0), RoutePoint(seq = 1, lat = 50.01, lon = 14.0)),
    )

    private fun assertNotNullAnd(value: String?): String {
        assertNotNull(value)
        return value!!
    }

    private class FakePhotoArchive : PhotoArchive {
        val files = linkedMapOf<String, ByteArray>()
        private var next = 0
        override fun exportBytes(path: String): ByteArray? = files[path]
        override fun exists(path: String): Boolean = path in files
        override fun stage(exerciseId: String, bytes: ByteArray): String =
            "/photos/$exerciseId-${next++}.jpg".also { files[it] = bytes }
        override fun delete(path: String) {
            files.remove(path)
        }
        override fun deleteAllExcept(keep: Set<String>) {
            files.keys.retainAll(keep)
        }
    }

    private class FakeSettingsArchive : SettingsArchive {
        var current = SettingsSnapshot()
        override suspend fun snapshot(): SettingsSnapshot = current
        override suspend fun restore(snapshot: SettingsSnapshot) {
            current = snapshot
        }
        override suspend fun reset() {
            current = SettingsSnapshot()
        }
    }
}
