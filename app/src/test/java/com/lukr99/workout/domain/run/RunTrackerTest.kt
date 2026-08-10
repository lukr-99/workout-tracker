package com.lukr99.workout.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour of the pure [RunTracker]: accuracy/jitter gating, distance accumulation, elapsed vs
 * moving time, auto-pause hysteresis, and split derivation — all deterministic (time is injected).
 */
class RunTrackerTest {

    private val base = 1_000_000L // arbitrary epoch-ms start

    /** ~100 m north of a base point ≈ 0.000899 deg latitude. */
    private fun north(lat: Double, meters: Double) = lat + meters / 111_320.0

    @Test
    fun accumulatesDistanceFromAcceptedFixes() {
        val t = RunTracker()
        t.start(base)
        var lat = 50.0
        val lon = 14.0
        // Five fixes, 100 m apart, one every 30 s → 500 m total.
        t.onSample(RunSample(base, lat, lon, accuracyM = 5.0, speedMps = 3.0))
        for (i in 1..5) {
            lat = north(lat, 100.0)
            t.onSample(RunSample(base + i * 30_000L, lat, lon, accuracyM = 5.0, speedMps = 3.0))
        }
        val s = t.snapshot()
        assertEquals(500.0, s.distanceMeters, 2.0)
        assertEquals(6, s.pointCount)
    }

    @Test
    fun rejectsInaccurateAndJitterFixes() {
        val t = RunTracker()
        t.start(base)
        t.onSample(RunSample(base, 50.0, 14.0, accuracyM = 5.0, speedMps = 3.0))
        // Way too inaccurate → rejected.
        val acceptedInaccurate = t.onSample(RunSample(base + 1_000, north(50.0, 100.0), 14.0, accuracyM = 80.0))
        // Real but < minMove (1 m) → rejected as jitter.
        val acceptedJitter = t.onSample(RunSample(base + 2_000, north(50.0, 1.0), 14.0, accuracyM = 5.0))
        assertFalse(acceptedInaccurate)
        assertFalse(acceptedJitter)
        assertEquals(0.0, t.snapshot().distanceMeters, 0.001)
        assertEquals(1, t.snapshot().pointCount)
    }

    @Test
    fun firstFixAnchorsEvenWhenInaccurate() {
        val t = RunTracker()
        t.start(base)
        // Indoors the only fix may be a coarse network location (100 m). It must still anchor the run
        // so a run always has a start point; the gate applies only to later fixes.
        val accepted = t.onSample(RunSample(base, 50.0, 14.0, accuracyM = 100.0))
        assertTrue(accepted)
        assertEquals(1, t.snapshot().pointCount)
        // A second, still-inaccurate fix is rejected (won't inflate distance).
        val second = t.onSample(RunSample(base + 5_000, north(50.0, 100.0), 14.0, accuracyM = 100.0))
        assertFalse(second)
        assertEquals(0.0, t.snapshot().distanceMeters, 0.001)
    }

    @Test
    fun movingTimeExcludesManualPause() {
        val t = RunTracker()
        t.start(base)
        t.tick(base + 10_000)         // 10 s recording
        t.pause(base + 10_000)
        t.tick(base + 40_000)         // 30 s paused — should not count
        t.resume(base + 40_000)
        t.tick(base + 50_000)         // 10 s recording
        val s = t.snapshot()
        assertEquals(20, s.elapsedSeconds) // 10 + 10, pause excluded
        assertEquals(20, s.movingSeconds)
    }

    @Test
    fun manualPauseThenWalkThenResumeNeitherConnectsNorCountsTheGap() {
        val t = RunTracker()
        t.start(base)
        var lat = 50.0
        val lon = 14.0
        // Run 500 m (a fix every 100 m / 30 s).
        t.onSample(RunSample(base, lat, lon, accuracyM = 5.0, speedMps = 3.0))
        for (i in 1..5) {
            lat = north(lat, 100.0)
            t.onSample(RunSample(base + i * 30_000L, lat, lon, accuracyM = 5.0, speedMps = 3.0))
        }
        assertEquals(500.0, t.snapshot().distanceMeters, 2.0)

        // Manually pause, then walk 300 m during the pause. A fix arriving while paused must not record.
        t.pause(base + 150_000)
        assertFalse(t.onSample(RunSample(base + 200_000, north(lat, 150.0), lon, accuracyM = 5.0, speedMps = 1.0)))
        val walkedTo = north(lat, 300.0)
        t.resume(base + 300_000)

        // First fix after resume anchors a fresh segment at the walked-to point — the 300 m gap is
        // dropped from the distance and the trace breaks rather than drawing across it.
        assertTrue(t.onSample(RunSample(base + 300_000, walkedTo, lon, accuracyM = 5.0, speedMps = 3.0)))
        assertEquals(500.0, t.snapshot().distanceMeters, 2.0)

        // Keep running 200 m in the new segment.
        var lat2 = walkedTo
        for (i in 1..2) {
            lat2 = north(lat2, 100.0)
            t.onSample(RunSample(base + 300_000 + i * 30_000L, lat2, lon, accuracyM = 5.0, speedMps = 3.0))
        }
        assertEquals(700.0, t.snapshot().distanceMeters, 3.0) // 500 + 200, the walked 300 m excluded

        val trace = t.trace()
        assertEquals(1, trace.count { it.segmentStart })     // exactly one break
        assertFalse(trace.first().segmentStart)              // never the very first point
        assertEquals(2, RunTrace.segments(trace).size)       // drawn as two disconnected lines
        assertEquals(700.0, Pace.traceDistanceMeters(trace), 3.0) // break-aware distance agrees
    }

    @Test
    fun autoPauseStopsMovingButNotElapsed() {
        val t = RunTracker(RunTracker.Config(autoPauseEnterMps = 0.6, autoPauseExitMps = 0.9))
        t.start(base)
        t.onSample(RunSample(base, 50.0, 14.0, accuracyM = 5.0, speedMps = 3.0))
        // A near-zero-speed fix far enough to be accepted spatially engages auto-pause.
        t.onSample(RunSample(base + 5_000, north(50.0, 100.0), 14.0, accuracyM = 5.0, speedMps = 0.1))
        assertTrue(t.snapshot().autoPaused)
        t.tick(base + 15_000) // 10 s while auto-paused
        val s = t.snapshot()
        assertEquals(15, s.elapsedSeconds)   // elapsed keeps running
        assertEquals(5, s.movingSeconds)     // moving froze at the auto-pause point
    }

    @Test
    fun splitsEmittedPerKilometre() {
        val t = RunTracker()
        t.start(base)
        var lat = 50.0
        val lon = 14.0
        t.onSample(RunSample(base, lat, lon, accuracyM = 5.0, speedMps = 3.33))
        // 2.5 km at a steady 5:00/km (300 s/km): a fix every 100 m / 30 s.
        for (i in 1..25) {
            lat = north(lat, 100.0)
            t.onSample(RunSample(base + i * 30_000L, lat, lon, accuracyM = 5.0, speedMps = 3.33))
        }
        val splits = t.splits(Pace.METERS_PER_KM)
        assertEquals(3, splits.size)                 // 2 full + remainder
        assertTrue(splits[0].isFull)
        assertEquals(300.0, splits[0].paceSecPerKm, 5.0)
        assertFalse(splits.last().isFull)            // trailing 0.5 km
    }

    @Test
    fun toRunCarriesFinalTotals() {
        val t = RunTracker()
        t.start(base)
        var lat = 50.0
        t.onSample(RunSample(base, lat, 14.0, accuracyM = 5.0, speedMps = 3.0))
        for (i in 1..10) {
            lat = north(lat, 100.0)
            t.onSample(RunSample(base + i * 30_000L, lat, 14.0, accuracyM = 5.0, speedMps = 3.0))
        }
        t.finish(base + 300_000)
        val run = t.toRun(id = "run-1")
        assertEquals(RunSource.Local, run.source)
        assertEquals(base, run.startedAtUtc)
        assertEquals(1000.0, run.distanceMeters, 5.0)
        assertTrue(run.trace.isNotEmpty())
        assertTrue(run.avgPaceSecPerKm > 0.0)
    }
}
