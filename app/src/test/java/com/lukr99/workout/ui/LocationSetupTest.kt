package com.lukr99.workout.ui

import com.lukr99.workout.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When Home shows the one-time "Where do you train?" card. */
class LocationSetupTest {

    @Test
    fun dueAfterTheUpdateWhenOldWorkoutsHaveNoPlace() {
        assertEquals(LocationSetup(12), LocationSetup.due(Location.builtIns, unknownWorkouts = 12, dismissed = false))
    }

    @Test
    fun notOnAFreshInstall() {
        assertNull(LocationSetup.due(Location.builtIns, unknownWorkouts = 0, dismissed = false))
    }

    @Test
    fun goneOnceAGymExistsOrTheOwnerSaidNotNow() {
        val withGym = Location.builtIns + Location(name = "Brno gym")
        assertNull(LocationSetup.due(withGym, unknownWorkouts = 12, dismissed = false))
        assertNull(LocationSetup.due(Location.builtIns, unknownWorkouts = 12, dismissed = true))
    }

    @Test
    fun anArchivedGymStillCounts() {
        val archived = Location.builtIns + Location(name = "Old gym", isArchived = true)
        assertNull(LocationSetup.due(archived, unknownWorkouts = 3, dismissed = false))
    }
}
