package com.lukr99.workout.ui.run

import com.lukr99.workout.ui.i18n.RunText
import kotlin.math.roundToInt

/** A fix this good or better is ready to run with; the tracker itself drops fixes past 30 m. */
internal const val GPS_READY_METRES = 20.0

/**
 * The line under "Start a run". Without location permission it describes the run, since the
 * permission is asked when the run starts; with it, it says how the GPS fix looks right now.
 * [hasFix] separates "no fix yet" from a fix that came without an accuracy.
 */
internal fun gpsLabel(permitted: Boolean, hasFix: Boolean, accuracyM: Double?): String = when {
    !permitted -> RunText.gpsDescribe.now()
    !hasFix -> RunText.gpsLooking.now()
    accuracyM == null -> RunText.gpsReady.now()
    accuracyM <= GPS_READY_METRES -> RunText.gpsReadyAccuracy.now("m" to accuracyM.roundToInt())
    else -> RunText.gpsWeak.now("m" to accuracyM.roundToInt())
}
