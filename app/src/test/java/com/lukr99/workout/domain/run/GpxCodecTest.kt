package com.lukr99.workout.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxCodecTest {

    // A short trace: three points ~100 m apart with elevation and 30 s spacing.
    private val trace = listOf(
        TracePoint(t = 0L, lat = 50.0000, lon = 14.4000, elevationM = 200.0),
        TracePoint(t = 30_000L, lat = 50.0009, lon = 14.4000, elevationM = 205.0),
        TracePoint(t = 60_000L, lat = 50.0018, lon = 14.4000, elevationM = 203.0),
    )
    private val run = Run(
        id = "run-1",
        startedAtUtc = 1_700_000_000_000L,
        durationSeconds = 60,
        movingSeconds = 60,
        distanceMeters = Pace.traceDistanceMeters(trace),
        source = RunSource.Local,
        notes = "Morning run",
        trace = trace,
    )

    @Test
    fun encode_producesGpx11WithTrackpoints() {
        val gpx = GpxCodec.encode(run)
        assertTrue(gpx.contains("""<gpx version="1.1""""))
        assertTrue(gpx.contains("http://www.topografix.com/GPX/1/1"))
        assertTrue(gpx.contains("<name>Morning run</name>"))
        assertEquals(3, Regex("<trkpt ").findAll(gpx).count())
        assertTrue(gpx.contains("<ele>200.000000</ele>"))
    }

    @Test
    fun roundTrip_preservesTraceGeometryAndTiming() {
        val gpx = GpxCodec.encode(run)
        val imported = GpxCodec.decode(gpx)
        assertNotNull(imported)
        imported!!
        assertEquals(RunSource.Imported, imported.source)
        assertEquals(run.startedAtUtc, imported.startedAtUtc)
        assertEquals(trace.size, imported.trace.size)
        for (i in trace.indices) {
            assertEquals(trace[i].lat, imported.trace[i].lat, 1e-5)
            assertEquals(trace[i].lon, imported.trace[i].lon, 1e-5)
            assertEquals(trace[i].elevationM!!, imported.trace[i].elevationM!!, 1e-3)
            assertEquals(trace[i].t, imported.trace[i].t)
        }
        assertEquals(run.distanceMeters, imported.distanceMeters, 0.5)
        assertEquals(60L, imported.durationSeconds)
        assertEquals("Morning run", imported.notes)
    }

    @Test
    fun decode_handlesExternalGpxWithoutElevation() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Strava" xmlns="http://www.topografix.com/GPX/1/1">
              <trk><name>Imported</name><trkseg>
                <trkpt lat="50.0" lon="14.4"><time>2023-11-14T22:13:20Z</time></trkpt>
                <trkpt lat="50.0009" lon="14.4"><time>2023-11-14T22:13:50Z</time></trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        val run = GpxCodec.decode(gpx)
        assertNotNull(run)
        run!!
        assertEquals(2, run.trace.size)
        assertNull(run.trace[0].elevationM)
        assertEquals(0L, run.trace[0].t)
        assertEquals(30_000L, run.trace[1].t)
        assertTrue(run.distanceMeters > 90.0)
    }

    @Test
    fun decode_missingTimes_keepsPointsWithZeroOffset() {
        val gpx = """
            <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
              <trk><trkseg>
                <trkpt lat="50.0" lon="14.4"/>
                <trkpt lat="50.001" lon="14.4"/>
              </trkseg></trk>
            </gpx>
        """.trimIndent()
        val run = GpxCodec.decode(gpx)
        assertNotNull(run)
        assertEquals(0L, run!!.trace[0].t)
        assertEquals(0L, run.trace[1].t)
        assertTrue(run.distanceMeters > 100.0)
    }

    @Test
    fun decode_rejectsNonGpxOrEmpty() {
        assertNull(GpxCodec.decode("not xml at all <<<"))
        assertNull(GpxCodec.decode("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"></gpx>"))
    }
}
