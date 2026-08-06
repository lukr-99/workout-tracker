package com.lukr99.workout.domain.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineTest {

    @Test
    fun encode_matchesGoogleReferenceVector() {
        // The canonical example from Google's polyline algorithm docs.
        val path = listOf(38.5 to -120.2, 40.7 to -120.95, 43.252 to -126.453)
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polyline.encode(path))
    }

    @Test
    fun decode_matchesGoogleReferenceVector() {
        val decoded = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        val expected = listOf(38.5 to -120.2, 40.7 to -120.95, 43.252 to -126.453)
        assertEquals(expected.size, decoded.size)
        expected.forEachIndexed { i, (lat, lon) ->
            assertEquals(lat, decoded[i].first, 1e-5)
            assertEquals(lon, decoded[i].second, 1e-5)
        }
    }

    @Test
    fun roundTrip_recoversCoordinatesWithinPrecision() {
        val path = listOf(
            50.0874654 to 14.4212535, // Prague-ish
            50.0880000 to 14.4200000,
            50.0890123 to 14.4180987,
            -33.8688 to 151.2093, // Sydney, southern/eastern hemisphere
        )
        val decoded = Polyline.decode(Polyline.encode(path))
        assertEquals(path.size, decoded.size)
        path.forEachIndexed { i, (lat, lon) ->
            assertEquals(lat, decoded[i].first, 1e-5)
            assertEquals(lon, decoded[i].second, 1e-5)
        }
    }

    @Test
    fun emptyPath_roundTrips() {
        assertEquals("", Polyline.encode(emptyList()))
        assertTrue(Polyline.decode("").isEmpty())
    }

    @Test
    fun encodeTrace_ordersByGivenSequence() {
        val trace = listOf(
            TracePoint(t = 0, lat = 50.08, lon = 14.42),
            TracePoint(t = 1000, lat = 50.081, lon = 14.421),
        )
        assertEquals(Polyline.encode(listOf(50.08 to 14.42, 50.081 to 14.421)), Polyline.encodeTrace(trace))
    }
}
