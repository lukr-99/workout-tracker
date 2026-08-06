package com.lukr99.workout.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceTest {

    // Along the equator (lat 0) haversine reduces to R·Δlon, so a longitude offset gives an exact,
    // predictable ground distance — handy for constructing traces with known split boundaries.
    private val earthR = 6_371_000.0
    private fun lonForMeters(m: Double): Double = Math.toDegrees(m / earthR)

    /** Straight eastward trace at [speedMps] m/s with a sample every [stepM] metres up to [totalM]. */
    private fun straightTrace(totalM: Double, stepM: Double, speedMps: Double): List<TracePoint> {
        val pts = ArrayList<TracePoint>()
        var d = 0.0
        while (d <= totalM + 1e-6) {
            pts += TracePoint(t = ((d / speedMps) * 1000).toLong(), lat = 0.0, lon = lonForMeters(d))
            d += stepM
        }
        return pts
    }

    @Test
    fun haversine_equatorDegreeIsKnownDistance() {
        // One degree of longitude at the equator ≈ 111.195 km for R = 6_371_000 m.
        assertEquals(111_194.93, Pace.haversineMeters(0.0, 0.0, 0.0, 1.0), 1.0)
        assertEquals(0.0, Pace.haversineMeters(1.0, 2.0, 1.0, 2.0), 1e-6)
    }

    @Test
    fun paceAndSpeed_knownValues() {
        assertEquals(300.0, Pace.paceSecPerKm(1000.0, 300.0), 1e-9) // 5:00 /km
        assertEquals(0.0, Pace.paceSecPerKm(0.0, 300.0), 1e-9)
        assertEquals(3.3333333, Pace.speedMps(1000.0, 300.0), 1e-6)
        assertEquals(0.0, Pace.speedMps(1000.0, 0.0), 1e-9)
        assertEquals(300.0 * Pace.METERS_PER_MILE / 1000.0, Pace.paceSecPerMile(300.0), 1e-6)
    }

    @Test
    fun traceDistance_matchesConstruction() {
        val trace = straightTrace(totalM = 3000.0, stepM = 500.0, speedMps = 10.0 / 3.0)
        assertEquals(3000.0, Pace.traceDistanceMeters(trace), 0.5)
    }

    @Test
    fun splits_exactMultipleHasNoRemainder() {
        // 3 km at 5:00/km (3.333 m/s) → three full 1 km splits of 300 s each.
        val trace = straightTrace(totalM = 3000.0, stepM = 500.0, speedMps = 10.0 / 3.0)
        val splits = Pace.splits(trace, Pace.METERS_PER_KM)
        assertEquals(3, splits.size)
        splits.forEachIndexed { i, s ->
            assertEquals(i + 1, s.index)
            assertTrue(s.isFull)
            assertEquals(1000.0, s.distanceMeters, 0.5)
            assertEquals(300L, s.durationSeconds)
            assertEquals(300.0, s.paceSecPerKm, 0.5)
        }
    }

    @Test
    fun splits_partialRemainderFlaggedNotFull() {
        // 2.5 km → two full km + a 500 m remainder.
        val trace = straightTrace(totalM = 2500.0, stepM = 250.0, speedMps = 10.0 / 3.0)
        val splits = Pace.splits(trace, Pace.METERS_PER_KM)
        assertEquals(3, splits.size)
        assertTrue(splits[0].isFull)
        assertTrue(splits[1].isFull)
        val last = splits[2]
        assertFalse(last.isFull)
        assertEquals(500.0, last.distanceMeters, 0.5)
        assertEquals(150L, last.durationSeconds)
    }

    @Test
    fun splits_emptyForTooFewPoints() {
        assertTrue(Pace.splits(emptyList()).isEmpty())
        assertTrue(Pace.splits(listOf(TracePoint(0, 0.0, 0.0))).isEmpty())
    }

    @Test
    fun elevationGain_sumsPositiveDeltasAboveThreshold() {
        val pts = listOf(10.0, 12.0, 11.0, 20.0).mapIndexed { i, e ->
            TracePoint(t = i.toLong(), lat = 0.0, lon = 0.0, elevationM = e)
        }
        assertEquals(11.0, Pace.elevationGainMeters(pts, thresholdM = 1.0), 1e-9)
        // A 0.5 m ripple below the threshold contributes nothing.
        val noisy = listOf(10.0, 10.4, 10.0, 10.3).mapIndexed { i, e ->
            TracePoint(t = i.toLong(), lat = 0.0, lon = 0.0, elevationM = e)
        }
        assertEquals(0.0, Pace.elevationGainMeters(noisy, thresholdM = 1.0), 1e-9)
    }

    @Test
    fun formatting_paceAndDuration() {
        assertEquals("5:00", Pace.formatPace(300.0))
        assertEquals("4:37", Pace.formatPace(277.0))
        assertEquals("--:--", Pace.formatPace(0.0))
        assertEquals("0:45", Pace.formatDuration(45))
        assertEquals("12:30", Pace.formatDuration(750))
        assertEquals("1:01:05", Pace.formatDuration(3665))
    }
}
