package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearestLocationTest {

    // Brno centre. One degree of latitude is about 111.2 km, so 0.001 degrees is about 111 m.
    private val here = GeoPoint(49.1951, 16.6068)

    private fun gym(id: String, name: String, dLat: Double = 0.0, dLng: Double = 0.0, archived: Boolean = false) =
        Location(id = id, name = name, kind = LocationKind.Gym, latitude = here.latitude + dLat, longitude = here.longitude + dLng, isArchived = archived)

    @Test
    fun distanceIsZeroForTheSamePointAndRoughlyRightNorthward() {
        assertEquals(0.0, distanceMeters(here, here), 0.001)
        assertEquals(111.2, distanceMeters(here, GeoPoint(here.latitude + 0.001, here.longitude)), 1.0)
    }

    @Test
    fun distanceShrinksEastwardAtThisLatitude() {
        val east = distanceMeters(here, GeoPoint(here.latitude, here.longitude + 0.001))
        assertTrue("east $east", east in 70.0..78.0)
    }

    @Test
    fun picksTheNearestWithinTheThreshold() {
        val far = gym("far", "Far gym", dLat = 0.001)
        val close = gym("close", "Close gym", dLat = 0.0004)
        assertEquals("close", listOf(far, close).nearest(here)?.location?.id)
    }

    @Test
    fun nothingBeyondTheThresholdMatches() {
        val place = gym("a", "A", dLat = 0.002)
        assertNull(listOf(place).nearest(here))
        assertEquals("a", listOf(place).nearest(here, maxMeters = 300.0)?.location?.id)
    }

    @Test
    fun theThresholdIsInclusive() {
        val place = gym("a", "A", dLat = 0.001)
        val meters = distanceMeters(here, place.point!!)
        assertEquals("a", listOf(place).nearest(here, maxMeters = meters)?.location?.id)
        assertNull(listOf(place).nearest(here, maxMeters = meters - 0.5))
    }

    @Test
    fun placesWithoutAPositionNeverMatch() {
        val nowhere = Location(id = "n", name = "No position", kind = LocationKind.Gym)
        val half = Location(id = "h", name = "Half", kind = LocationKind.Gym, latitude = here.latitude)
        assertNull(listOf(nowhere, half).nearest(here))
        assertNull(emptyList<Location>().nearest(here))
    }

    @Test
    fun outdoorsIsExcludedEvenWithAPosition() {
        val outdoors = Location.builtIns.first { it.kind == LocationKind.Outdoors }
            .copy(latitude = here.latitude, longitude = here.longitude)
        assertNull(listOf(outdoors).nearest(here))
        val gym = gym("g", "Gym", dLat = 0.001)
        assertEquals("g", listOf(outdoors, gym).nearest(here)?.location?.id)
    }

    @Test
    fun homeCanMatch() {
        val home = Location.builtIns.first { it.kind == LocationKind.Home }
            .copy(latitude = here.latitude, longitude = here.longitude)
        assertEquals(Location.HOME_ID, listOf(home).nearest(here)?.location?.id)
    }

    @Test
    fun archivedPlacesAreSkipped() {
        val old = gym("old", "Old", archived = true)
        val current = gym("new", "New", dLat = 0.001)
        assertEquals("new", listOf(old, current).nearest(here)?.location?.id)
        assertNull(listOf(old).nearest(here))
    }

    @Test
    fun anExactTieGoesToTheFirstNameThenIdWhateverTheListOrder() {
        val b = gym("1", "Beta", dLat = 0.0005)
        val a = gym("2", "alpha", dLat = 0.0005)
        assertEquals("2", listOf(b, a).nearest(here)?.location?.id)
        assertEquals("2", listOf(a, b).nearest(here)?.location?.id)
        val twin1 = gym("x1", "Same", dLat = 0.0005)
        val twin2 = gym("x2", "Same", dLat = 0.0005)
        assertEquals("x1", listOf(twin2, twin1).nearest(here)?.location?.id)
    }

    @Test
    fun switchOnlyForAMatchThatDiffersAndWasNotPickedByHand() {
        val near = gym("a", "A")
        assertEquals("a", startLocationSwitch(currentId = "other", pickedByHand = false, near = near))
        assertEquals("a", startLocationSwitch(currentId = null, pickedByHand = false, near = near))
        assertNull(startLocationSwitch(currentId = "a", pickedByHand = false, near = near))
        assertNull(startLocationSwitch(currentId = "other", pickedByHand = true, near = near))
        assertNull(startLocationSwitch(currentId = "other", pickedByHand = false, near = null))
    }

    @Test
    fun offerOnlyForAGymOrHomeWithoutAPositionThatWasNotAnsweredAndHasNoMatch() {
        val bare = Location(id = "g", name = "G", kind = LocationKind.Gym)
        assertEquals(bare, positionOfferFor(bare, near = null, answered = emptySet()))
        assertNull(positionOfferFor(bare, near = null, answered = setOf("g")))
        assertNull(positionOfferFor(bare, near = gym("x", "X"), answered = emptySet()))
        assertNull(positionOfferFor(gym("p", "Placed"), near = null, answered = emptySet()))
        assertNull(positionOfferFor(bare.copy(isArchived = true), near = null, answered = emptySet()))
        assertNull(positionOfferFor(Location.builtIns.first { it.kind == LocationKind.Outdoors }, null, emptySet()))
        assertNull(positionOfferFor(null, near = null, answered = emptySet()))
    }
}
