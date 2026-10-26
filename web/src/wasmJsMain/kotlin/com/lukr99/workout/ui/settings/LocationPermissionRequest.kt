package com.lukr99.workout.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * A web page gets no position while it is in the background, so the web edition does not offer to
 * save a gym's position or suggest gyms by position.
 */
const val CAN_SAVE_POSITIONS: Boolean = false

/**
 * Returns a function that asks for location access and reports the answer to [onResult].
 *
 * Each platform supplies this file. The web edition has no GPS features in this version, so it never
 * asks the browser and reports [LocationPermissionResult.Denied] straight away.
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (LocationPermissionResult) -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onResult)
    return remember { { latest.value(LocationPermissionResult.Denied) } }
}
