package com.lukr99.workout.domain

/** Gyms first, then Home and Outdoors, each group by name. Archived places are left out. */
fun List<Location>.forPicker(): List<Location> =
    filterNot(Location::isArchived)
        .sortedWith(compareBy<Location> { it.kind.ordinal }.thenBy { it.name.lowercase() })

/** The display name of [locationId], or [Location.UNKNOWN_NAME] when it is null or not found. */
fun List<Location>.nameOf(locationId: String?): String =
    locationId?.let { id -> firstOrNull { it.id == id }?.name } ?: Location.UNKNOWN_NAME
