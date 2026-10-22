package com.lukr99.workout.data.location

import com.lukr99.workout.domain.GeoPoint
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationSuggesterTest {

    private val gym = Location(id = "g", name = "Brno gym", kind = LocationKind.Gym, latitude = 49.1951, longitude = 16.6068)
    private val places = listOf(gym) + Location.builtIns

    private class FakeProvider(
        private val granted: Boolean = true,
        private val fix: GeoPoint? = null,
        private val fails: Boolean = false,
    ) : PositionProvider {
        var asked = 0
        var lastQuality: FixQuality? = null
        override fun hasPermission() = granted
        override suspend fun currentPosition(quality: FixQuality, timeoutMs: Long): GeoPoint? {
            asked++
            lastQuality = quality
            if (fails) error("engine blew up")
            return fix
        }
    }

    @Test
    fun aFixNearASavedPlaceNamesIt() = runTest {
        val provider = FakeProvider(fix = GeoPoint(49.1955, 16.6070))
        val result = LocationSuggester(provider).suggest(places)
        assertEquals("g", result?.near?.id)
        assertEquals(FixQuality.Balanced, provider.lastQuality)
    }

    @Test
    fun aFixFarFromEverythingKeepsTheFixButNamesNoPlace() = runTest {
        val far = GeoPoint(50.0, 14.4)
        val result = LocationSuggester(FakeProvider(fix = far)).suggest(places)
        assertEquals(far, result?.fix)
        assertNull(result?.near)
    }

    @Test
    fun theThresholdIsTunable() = runTest {
        val fix = GeoPoint(49.1951 + 0.001, 16.6068) // about 111 m away
        assertEquals("g", LocationSuggester(FakeProvider(fix = fix)).suggest(places)?.near?.id)
        assertNull(LocationSuggester(FakeProvider(fix = fix), maxMeters = 50.0).suggest(places)?.near)
    }

    @Test
    fun withoutPermissionItNeverAsksTheProvider() = runTest {
        val provider = FakeProvider(granted = false, fix = GeoPoint(49.1951, 16.6068))
        assertNull(LocationSuggester(provider).suggest(places))
        assertNull(LocationSuggester(provider).currentFix())
        assertEquals(0, provider.asked)
    }

    @Test
    fun noFixMeansNoSuggestion() = runTest {
        assertNull(LocationSuggester(FakeProvider(fix = null)).suggest(places))
    }

    @Test
    fun aProviderThatThrowsFallsBackToNothing() = runTest {
        assertNull(LocationSuggester(FakeProvider(fails = true)).suggest(places))
        assertNull(LocationSuggester(FakeProvider(fails = true)).currentFix())
    }

    @Test
    fun savingAPlaceAsksForTheBetterFix() = runTest {
        val provider = FakeProvider(fix = GeoPoint(1.0, 2.0))
        assertEquals(GeoPoint(1.0, 2.0), LocationSuggester(provider).currentFix())
        assertEquals(FixQuality.High, provider.lastQuality)
    }

    @Test
    fun noPositionProviderNeverHasPermission() = runTest {
        assertNull(LocationSuggester(NoPositionProvider).suggest(places))
    }
}
