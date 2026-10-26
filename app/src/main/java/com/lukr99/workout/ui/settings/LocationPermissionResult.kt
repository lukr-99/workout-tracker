package com.lukr99.workout.ui.settings

/** What the person allowed when asked for location access. */
enum class LocationPermissionResult {
    /** Precise location: enough to save the position of a gym. */
    Precise,

    /** Only approximate location, which is too coarse to save a position. */
    ApproximateOnly,

    /** No location access. */
    Denied,
}
