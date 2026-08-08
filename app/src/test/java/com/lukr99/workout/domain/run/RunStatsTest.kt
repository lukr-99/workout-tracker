package com.lukr99.workout.domain.run

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunStatsTest {

    private val utc = ZoneOffset.UTC

    /** A run whose trace is a straight northward line of [meters] covered in [seconds] at even pace. */
    private fun straightRun(
        id: String,
        startedAtUtc: Long,
        meters: Double,
        seconds: Long,
        elevation: Double = 0.0,
    ): Run {
        val steps = 20
        // 111_000 m/deg slightly overshoots the true ~111.2 km/deg so the haversine trace length
        // meets/exceeds the nominal [meters] (a real "fastest 5k" needs ≥5000 m of actual trace).
        val trace = (0..steps).map { i ->
            TracePoint(
                t = (seconds * 1000L * i) / steps,
                lat = 50.0 + (meters * i / steps) / 111_000.0,
                lon = 14.0,
                elevationM = elevation * i / steps,
            )
        }
        val dist = Pace.traceDistanceMeters(trace)
        return Run(
            id = id,
            startedAtUtc = startedAtUtc,
            durationSeconds = seconds,
            movingSeconds = seconds,
            distanceMeters = dist,
            avgPaceSecPerKm = Pace.paceSecPerKm(dist, seconds.toDouble()),
            elevationGainM = Pace.elevationGainMeters(trace),
            trace = trace,
        )
    }

    private fun daysAgo(days: Long, now: Long): Long = now - days * 24 * 3600 * 1000

    @Test
    fun summarize_firstRunEverAwardsNoHollowRecords() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val only = straightRun("first", now, 5_000.0, 1_500)
        val summary = RunStats.summarize(only, listOf(only))
        // A lone run trivially "holds" every record — don't call them out.
        assertTrue(summary.newRecords.isEmpty())
        assertEquals(only.movingSeconds, summary.movingSeconds)
    }

    @Test
    fun summarize_flagsRecordsTheNewRunActuallyBeat() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val old = straightRun("old", daysAgo(10, now), 5_000.0, 1_800) // 6:00/km, 5 km
        val faster = straightRun("new", now, 6_000.0, 1_500)           // 5:00/km, longer + faster
        val summary = RunStats.summarize(faster, listOf(old, faster))
        // The new run is longest, has the best avg pace, and owns the fastest 1k/5k windows.
        assertTrue(RunStats.PrKind.LongestRun in summary.newRecords)
        assertTrue(RunStats.PrKind.BestAvgPace in summary.newRecords)
        assertTrue(RunStats.PrKind.Fastest5k in summary.newRecords)
    }

    @Test
    fun summarize_awardsNothingWhenTheRunBeatsNoRecord() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val best = straightRun("best", daysAgo(10, now), 10_000.0, 2_400) // 4:00/km, 10 km
        val slowShort = straightRun("new", now, 2_000.0, 720)             // 6:00/km, 2 km — beats nothing
        val summary = RunStats.summarize(slowShort, listOf(best, slowShort))
        assertTrue(summary.newRecords.isEmpty())
    }

    @Test
    fun totalsSumAndWeightPace() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val runs = listOf(
            straightRun("a", daysAgo(1, now), 5_000.0, 1_500), // 5:00/km
            straightRun("b", daysAgo(2, now), 5_000.0, 1_800), // 6:00/km
        )
        val t = RunStats.totals(runs)
        assertEquals(2, t.runCount)
        assertEquals(10_000.0, t.distanceMeters, 20.0)
        assertEquals(3_300, t.movingSeconds)
        // Distance-weighted pace over 10 km in 3300 s = 5:30/km = 330 s/km.
        assertEquals(330.0, t.avgPaceSecPerKm, 2.0)
    }

    @Test
    fun weeklyDistanceBucketsIncludeEmptyWeeks() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli() // a Saturday
        val runs = listOf(
            straightRun("thisWeek", now - 2L * 24 * 3600 * 1000, 3_000.0, 900),
            straightRun("threeWeeksAgo", now - 21L * 24 * 3600 * 1000, 4_000.0, 1_200),
        )
        val weekly = RunStats.weeklyDistance(runs, weeks = 4, zone = utc, nowUtc = now)
        assertEquals(4, weekly.size)
        // Oldest→newest: [3 weeks ago]=4000, [2 ago]=0, [1 ago]=0, [this]=3000
        assertEquals(4_000.0, weekly[0].distanceMeters, 20.0)
        assertEquals(0.0, weekly[1].distanceMeters, 0.001)
        assertEquals(0.0, weekly[2].distanceMeters, 0.001)
        assertEquals(3_000.0, weekly[3].distanceMeters, 20.0)
    }

    @Test
    fun paceTrendIsChronologicalAndSkipsZeroDistance() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val older = straightRun("old", daysAgo(5, now), 2_000.0, 600)
        val newer = straightRun("new", daysAgo(1, now), 2_000.0, 720)
        val empty = Run(id = "empty", startedAtUtc = daysAgo(3, now)) // no distance
        val trend = RunStats.paceTrend(listOf(newer, empty, older))
        assertEquals(2, trend.size)
        assertEquals(older.startedAtUtc, trend[0].first)
        assertEquals(newer.startedAtUtc, trend[1].first)
    }

    @Test
    fun currentStreakCountsConsecutiveWeeks() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val runs = listOf(
            straightRun("w0", now - 1L * 24 * 3600 * 1000, 1_000.0, 300),
            straightRun("w1", now - 8L * 24 * 3600 * 1000, 1_000.0, 300),
            // skip week 2
            straightRun("w3", now - 22L * 24 * 3600 * 1000, 1_000.0, 300),
        )
        assertEquals(2, RunStats.currentStreakWeeks(runs, zone = utc, nowUtc = now))
    }

    @Test
    fun fastestWindowFindsBestSubSegment() {
        // A 2 km run: first km slow (400 s), second km fast (240 s). Best 1 km ≈ 240 s → 4:00/km.
        val trace = ArrayList<TracePoint>()
        var lat = 50.0
        var tMs = 0L
        trace += TracePoint(t = 0, lat = lat, lon = 14.0)
        // first km: 10 x 100 m @ 40 s each
        repeat(10) {
            lat += 100.0 / 111_320.0; tMs += 40_000
            trace += TracePoint(t = tMs, lat = lat, lon = 14.0)
        }
        // second km: 10 x 100 m @ 24 s each
        repeat(10) {
            lat += 100.0 / 111_320.0; tMs += 24_000
            trace += TracePoint(t = tMs, lat = lat, lon = 14.0)
        }
        val ms = RunStats.fastestTimeForDistanceMs(trace, 1_000.0)
        assertNotNull(ms)
        assertEquals(240_000.0, ms!!, 3_000.0) // ~240 s for the fast km
    }

    @Test
    fun personalRecordsPicksFastestAndLongest() {
        val now = Instant.parse("2026-08-08T10:00:00Z").toEpochMilli()
        val fast5k = straightRun("fast", daysAgo(2, now), 5_000.0, 1_200) // 4:00/km
        val slow5k = straightRun("slow", daysAgo(4, now), 5_000.0, 1_800) // 6:00/km
        val long10k = straightRun("long", daysAgo(6, now), 10_000.0, 3_600) // 6:00/km, longest
        val prs = RunStats.personalRecords(listOf(fast5k, slow5k, long10k))
        assertNotNull(prs.fastest1k)
        assertNotNull(prs.fastest5k)
        assertEquals("fast", prs.fastest5k!!.runId) // fastest 5k is the 4:00/km run
        assertEquals("long", prs.longestRun!!.runId)
        assertEquals(10_000.0, prs.longestRun!!.value, 30.0)
        // No run reaches a half marathon.
        assertNull(prs.fastestHalf)
        // Best average pace over >=1 km: the fast 5k.
        assertEquals("fast", prs.bestAvgPace!!.runId)
    }

    @Test
    fun emptyInputsAreSafe() {
        assertEquals(0, RunStats.totals(emptyList()).runCount)
        assertTrue(RunStats.paceTrend(emptyList()).isEmpty())
        assertEquals(0, RunStats.currentStreakWeeks(emptyList(), zone = utc, nowUtc = 0))
        val prs = RunStats.personalRecords(emptyList())
        assertNull(prs.fastest5k)
        assertNull(prs.longestRun)
    }
}
