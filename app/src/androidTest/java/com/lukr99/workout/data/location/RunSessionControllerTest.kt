package com.lukr99.workout.data.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.RunTracker
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end, **fully automatic** exercise of the live-run data path — GPS samples → [RunTracker] →
 * [RunSessionController] → [RunRepository] → Room — with a synthetic route and a controllable clock.
 * This is the repeatable substitute for driving a run by hand on the device: assert distance, points,
 * pace, idle-reset, discard, and crash-buffer recovery without a real GPS fix or a screenshot.
 */
@RunWith(AndroidJUnit4::class)
class RunSessionControllerTest {

    private lateinit var db: WorkoutDb
    private lateinit var repository: RunRepository
    private lateinit var controller: RunSessionController
    private var now = T0

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val bufferFile = File(context.filesDir, "run_crash_buffer.json")

    @Before
    fun setUp() {
        bufferFile.delete()
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().build()
        repository = RunRepository(db.runDao())
        controller = RunSessionController(context, repository, clock = { now })
    }

    @After
    fun tearDown() {
        db.close()
        bufferFile.delete()
    }

    /** Feed a straight 1 km line as 21 fixes 50 m / 15 s apart (≈5:00/km), then finish + assert. */
    @Test
    fun recordsAndSavesRun() = runBlocking {
        now = T0
        controller.start()
        var lat = 50.0
        val lon = 14.0
        controller.onLocation(lat, lon, accuracyM = 5.0, speedMps = 3.3, elevationM = 200.0, timeMs = now)
        repeat(20) {
            lat += 50.0 / 111_320.0 // ~50 m north
            now += 15_000
            controller.onLocation(lat, lon, accuracyM = 5.0, speedMps = 3.3, elevationM = 200.0, timeMs = now)
        }
        val run = controller.finish()

        assertEquals(1000.0, run.distanceMeters, 30.0)
        assertEquals(21, run.trace.size)
        assertEquals(300.0, run.avgPaceSecPerKm, 15.0) // ~5:00/km
        assertTrue(run.encodedPolyline.isNotEmpty())

        // Persisted and reloadable with its trace.
        val stored = repository.getRuns()
        assertEquals(1, stored.size)
        assertEquals(run.id, stored.first().id)
        assertEquals(21, repository.getRun(run.id)!!.trace.size)

        // Controller reset to idle so the next run starts clean (the stale-state bug).
        assertEquals(RunTracker.Phase.Idle, controller.state.value.phase)
        assertTrue(!controller.isRunning)
    }

    @Test
    fun discardSavesNothingAndResets() = runBlocking {
        now = T0
        controller.start()
        controller.onLocation(50.0, 14.0, accuracyM = 5.0, speedMps = 3.0, elevationM = null, timeMs = now)
        controller.discard()

        assertEquals(0, repository.countRuns())
        assertEquals(RunTracker.Phase.Idle, controller.state.value.phase)
    }

    /** A crash buffer left by a killed process is salvaged into a saved run on the next launch. */
    @Test
    fun recoversCrashBuffer() = runBlocking {
        now = T0
        controller.start()
        var lat = 50.0
        controller.onLocation(lat, 14.0, accuracyM = 5.0, speedMps = 3.0, elevationM = null, timeMs = now)
        repeat(10) {
            lat += 50.0 / 111_320.0
            now += 10_000
            controller.onLocation(lat, 14.0, accuracyM = 5.0, speedMps = 3.0, elevationM = null, timeMs = now)
        }
        // Simulate process death: never finish; wait for the async buffer flush to hit disk.
        waitForBuffer()

        // A fresh controller (new process) recovers it.
        val recovered = RunSessionController(context, repository, clock = { now })
        val run = recovered.recoverIfNeeded()

        assertTrue(run != null)
        assertEquals(1, repository.countRuns())
        assertTrue(run!!.distanceMeters > 400.0) // ~500 m of the 11 points
        assertTrue(run.notes.contains("Recovered"))
        assertTrue(!bufferFile.exists())
    }

    /** Wait until the async crash-buffer flushes have all landed (file present and its size stable). */
    private fun waitForBuffer() {
        val deadline = System.currentTimeMillis() + 5_000
        while (!bufferFile.exists() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue("crash buffer was never written", bufferFile.exists())
        var last = -1L
        while (System.currentTimeMillis() < deadline) {
            val len = bufferFile.length()
            if (len == last) return // size settled → all flushes landed
            last = len
            Thread.sleep(150)
        }
    }

    private companion object {
        const val T0 = 1_700_000_000_000L
    }
}
