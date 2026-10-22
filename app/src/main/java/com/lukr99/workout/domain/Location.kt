package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/**
 * A place where the owner trains: a named gym, or the built-in Home and Outdoors. A workout points
 * at one by [WorkoutSession.locationId]; null there means "Unknown location".
 *
 * Locations are archived, never deleted, so old workouts keep their place. [latitude] and
 * [longitude] are optional and unused for now; a later GPS suggestion fills them.
 */
@Serializable
data class Location(
    val id: String = newId(),
    val name: String = "",
    val kind: LocationKind = LocationKind.Gym,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isArchived: Boolean = false,
) {
    val isBuiltIn: Boolean get() = id == HOME_ID || id == OUTDOORS_ID

    companion object {
        /** Fixed ids, so Home and Outdoors stay the same row across devices and restores. */
        const val HOME_ID = "home"
        const val OUTDOORS_ID = "outdoors"

        /** Shown for workouts that have no location. */
        const val UNKNOWN_NAME = "Unknown location"

        val builtIns: List<Location> = listOf(
            Location(id = HOME_ID, name = "Home", kind = LocationKind.Home),
            Location(id = OUTDOORS_ID, name = "Outdoors", kind = LocationKind.Outdoors),
        )
    }
}
