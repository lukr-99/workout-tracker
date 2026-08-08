package com.lukr99.workout.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDeviationTest {

    // A short E–W route near Prague along a constant latitude.
    private val route = listOf(
        50.0876 to 14.4200,
        50.0876 to 14.4260,
    )

    @Test
    fun zeroWhenOnTheRoute() {
        val d = RouteDeviation.distanceToRouteMeters(50.0876, 14.4230, route)
        assertEquals(0.0, d, 1.5)
    }

    @Test
    fun measuresPerpendicularOffset() {
        // ~100 m north of the line (0.000904 deg lat ≈ 100 m).
        val d = RouteDeviation.distanceToRouteMeters(50.0876 + 0.000904, 14.4230, route)
        assertEquals(100.0, d, 5.0)
    }

    @Test
    fun clampsToNearestVertexBeyondEnds() {
        // Well east of the route's end — nearest point is the eastern vertex.
        val d = RouteDeviation.distanceToRouteMeters(50.0876, 14.4300, route)
        assertTrue(d > 200.0)
    }

    @Test
    fun infiniteForEmptyRoute() {
        assertTrue(RouteDeviation.distanceToRouteMeters(50.0, 14.0, emptyList()).isInfinite())
    }
}
