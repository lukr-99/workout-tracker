package com.lukr99.workout.data.routing

import com.lukr99.workout.domain.run.Polyline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmRouteParserTest {

    /** Encode a known path so the fixture's geometry decodes back to it. */
    private val path = listOf(
        50.0876 to 14.4207,
        50.0900 to 14.4230,
        50.0925 to 14.4260,
    )
    private val geometry = Polyline.encode(path)

    private fun okResponse(distance: Double = 640.0): String =
        """{"code":"Ok","routes":[{"geometry":"$geometry","distance":$distance,"duration":500.0}],"waypoints":[]}"""

    @Test
    fun parsesGeometryDistanceAndSequencesPoints() {
        val route = OsrmRouteParser.parse(okResponse(distance = 640.0))!!
        assertEquals(640.0, route.distanceMeters, 0.001)
        assertEquals(path.size, route.points.size)
        assertEquals(0, route.points.first().seq)
        assertEquals(path.lastIndex, route.points.last().seq)
        assertEquals(50.0876, route.points.first().lat, 1e-4)
        assertEquals(14.4207, route.points.first().lon, 1e-4)
    }

    @Test
    fun fallsBackToComputedDistanceWhenMissing() {
        val route = OsrmRouteParser.parse(okResponse(distance = 0.0))!!
        assertTrue(route.distanceMeters > 0.0) // haversine over the decoded path
    }

    @Test
    fun rejectsNonOkOrEmpty() {
        assertNull(OsrmRouteParser.parse("""{"code":"NoRoute","routes":[]}"""))
        assertNull(OsrmRouteParser.parse("""{"code":"Ok","routes":[]}"""))
        assertNull(OsrmRouteParser.parse("not json"))
    }
}
