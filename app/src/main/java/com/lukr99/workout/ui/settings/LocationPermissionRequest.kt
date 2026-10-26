package com.lukr99.workout.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Returns a function that asks for location access and reports the answer to [onResult].
 *
 * Each platform supplies this file; the web edition's is under web/.
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (LocationPermissionResult) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onResult(
            when {
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true -> LocationPermissionResult.Precise
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> LocationPermissionResult.ApproximateOnly
                else -> LocationPermissionResult.Denied
            },
        )
    }
    return {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}
