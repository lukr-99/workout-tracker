package com.lukr99.workout.ui.run

import kotlin.math.roundToInt

/** A fix this good or better is ready to run with; the tracker itself drops fixes past 30 m. */
internal const val GPS_READY_METRES = 20.0

/**
 * The line under "Start a run". Without location permission it describes the run, since the
 * permission is asked when the run starts; with it, it says how the GPS fix looks right now.
 * [hasFix] separates "no fix yet" from a fix that came without an accuracy.
 */
internal fun gpsLabel(permitted: Boolean, hasFix: Boolean, accuracyM: Double?): String = when {
    !permitted -> "GPS route, pace and splits"
    !hasFix -> "Looking for GPS…"
    accuracyM == null -> "GPS ready"
    accuracyM <= GPS_READY_METRES -> "GPS ready · ±${accuracyM.roundToInt()} m"
    else -> "Weak GPS · ±${accuracyM.roundToInt()} m. Open sky helps."
}
