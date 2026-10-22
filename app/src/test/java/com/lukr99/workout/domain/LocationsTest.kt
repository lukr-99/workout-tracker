package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationsTest {

    private val places = listOf(
        Location(id = "z", name = "Zlin gym"),
        Location(id = "old", name = "Closed gym", isArchived = true),
    ) + Location.builtIns + Location(id = "b", name = "brno gym")

    @Test
    fun pickerListsGymsThenHomeThenOutdoorsWithoutArchived() {
        assertEquals(listOf("brno gym", "Zlin gym", "Home", "Outdoors"), places.forPicker().map { it.name })
    }

    @Test
    fun namesFallBackToUnknown() {
        assertEquals("Zlin gym", places.nameOf("z"))
        assertEquals("Closed gym", places.nameOf("old"))
        assertEquals(Location.UNKNOWN_NAME, places.nameOf(null))
        assertEquals(Location.UNKNOWN_NAME, places.nameOf("missing"))
    }

    @Test
    fun builtInsHaveFixedIdsAndKinds() {
        assertEquals(listOf(LocationKind.Home, LocationKind.Outdoors), Location.builtIns.map { it.kind })
        assertTrue(Location.builtIns.all { it.isBuiltIn })
        assertFalse(Location(name = "Gym").isBuiltIn)
    }
}
