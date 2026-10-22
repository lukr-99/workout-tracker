package com.lukr99.workout.ui

import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.LocationKind

/**
 * Home's one-time "Where do you train?" card: [unknownWorkouts] finished workouts have no location
 * yet and can all be given the main gym.
 */
data class LocationSetup(val unknownWorkouts: Int) {
    companion object {
        /**
         * Due after the update that added locations: there are finished workouts without one, the
         * owner has not named a gym yet, and has not put the card away.
         */
        fun due(locations: List<Location>, unknownWorkouts: Int, dismissed: Boolean): LocationSetup? = when {
            dismissed || unknownWorkouts <= 0 -> null
            locations.any { it.kind == LocationKind.Gym } -> null
            else -> LocationSetup(unknownWorkouts)
        }
    }
}
