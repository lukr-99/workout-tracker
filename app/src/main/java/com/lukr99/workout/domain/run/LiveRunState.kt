package com.lukr99.workout.domain.run

/** Immutable live-run readout derived by [RunTracker], observed by the UI and notification. */
data class LiveRunState(
    val phase: RunTracker.Phase = RunTracker.Phase.Idle,
    val startedAtUtc: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val movingSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val autoPaused: Boolean = false,
    val pointCount: Int = 0,
    val lastLat: Double? = null,
    val lastLon: Double? = null,
) {
    val isActive: Boolean get() = phase == RunTracker.Phase.Recording || phase == RunTracker.Phase.Paused
}
