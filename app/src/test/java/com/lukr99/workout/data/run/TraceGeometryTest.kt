package com.lukr99.workout.data.run

import com.lukr99.workout.domain.run.TracePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceGeometryTest {

    private fun p(lat: Double, lon: Double, segmentStart: Boolean = false) =
        TracePoint(t = 0L, lat = lat, lon = lon, segmentStart = segmentStart)

    private fun assertPose(x: Float, y: Float, angle: Float, pose: TracePose?) {
        assertEquals(x, pose!!.x, 0.01f)
        assertEquals(y, pose.y, 0.01f)
        assertEquals(angle, pose.angleDegrees, 0.01f)
    }

    @Test
    fun project_keepsAspectAndCentresTheTrace() {
        // About 1.1 km north and no east-west span: a vertical line centred in a wide box.
        val lines = TraceGeometry.project(listOf(p(49.0, 16.0), p(49.01, 16.0)), 0f, 0f, 400f, 200f)
        val (start, end) = lines.single()
        assertEquals(200f, start.first, 0.5f)
        assertEquals(200f, start.second, 0.5f) // south is at the bottom
        assertEquals(0f, end.second, 0.5f)
    }

    @Test
    fun project_breaksAtPauses() {
        val lines = TraceGeometry.project(
            listOf(p(49.0, 16.0), p(49.001, 16.0), p(49.002, 16.001, segmentStart = true), p(49.003, 16.001)),
            0f, 0f, 100f, 100f,
        )
        assertEquals(2, lines.size)
        assertTrue(TraceGeometry.project(listOf(p(49.0, 16.0)), 0f, 0f, 100f, 100f).isEmpty())
    }

    @Test
    fun poseAlong_findsThePointAndDirection() {
        // Right 100, then down 100 (canvas y grows downwards).
        val line = listOf(listOf(0f to 0f, 100f to 0f, 100f to 100f))
        assertEquals(200f, TraceGeometry.length(line), 0.001f)
        assertPose(50f, 0f, 0f, TraceGeometry.poseAlong(line, 0.25f))
        assertPose(100f, 50f, 90f, TraceGeometry.poseAlong(line, 0.75f))
        assertNull(TraceGeometry.poseAlong(listOf(listOf(5f to 5f, 5f to 5f)), 0.5f))
    }

    @Test
    fun poseAlong_skipsTheGapBetweenSegments() {
        val lines = listOf(listOf(0f to 0f, 100f to 0f), listOf(500f to 0f, 500f to 100f))
        assertPose(500f, 50f, 90f, TraceGeometry.poseAlong(lines, 0.75f))
    }

    @Test
    fun startMeetsFinish_onlyWhenClose() {
        assertTrue(TraceGeometry.startMeetsFinish(0f to 0f, 30f to 30f, threshold = 48f))
        assertFalse(TraceGeometry.startMeetsFinish(0f to 0f, 40f to 40f, threshold = 48f))
    }
}
