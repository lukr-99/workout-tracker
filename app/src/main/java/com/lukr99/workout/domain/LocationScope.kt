package com.lukr99.workout.domain

/**
 * Which workouts count towards one exercise's numbers: last time's sets, records, suggestions and
 * progress. A shared exercise counts every workout. An exercise tracked per location counts only
 * workouts at [locationId]; when that is null, only workouts with no location count.
 *
 * Nothing is rewritten when an exercise switches mode. The history stays as it is and is only read
 * through a different scope, so switching back to shared joins the views again.
 */
data class LocationScope(
    val perLocation: Boolean,
    val locationId: String?,
) {
    fun includes(session: WorkoutSession): Boolean = !perLocation || session.locationId == locationId

    fun apply(sessions: List<WorkoutSession>): List<WorkoutSession> =
        if (perLocation) sessions.filter(::includes) else sessions

    companion object {
        /** Every workout counts. */
        val Shared = LocationScope(perLocation = false, locationId = null)

        fun of(exercise: Exercise?, locationId: String?): LocationScope =
            if (exercise?.trackPerLocation == true) LocationScope(true, locationId) else Shared
    }
}
