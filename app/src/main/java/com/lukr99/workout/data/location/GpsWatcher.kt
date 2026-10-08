package com.lukr99.workout.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * How good the GPS fix is before a run starts, for the Start a run card. It listens only while the
 * flow is collected, so the card collects it while the app is in front. Fixes older than
 * [MAX_AGE_MS] are ignored: a stale last-known location says nothing about the sky right now.
 */
class GpsWatcher(private val context: Context) {

    /** The accuracy in metres of each fresh fix, or null for a fix without one. Needs fine location. */
    @SuppressLint("MissingPermission") // Collected only after ACCESS_FINE_LOCATION is granted.
    fun accuracy(): Flow<Double?> = callbackFlow {
        val engine = LocationEngineDefault.getDefaultLocationEngine(context.applicationContext)
        val callback = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                val fix = result?.lastLocation ?: return
                if (System.currentTimeMillis() - fix.time > MAX_AGE_MS) return
                trySend(if (fix.hasAccuracy()) fix.accuracy.toDouble() else null)
            }

            override fun onFailure(exception: Exception) = Unit
        }
        val request = LocationEngineRequest.Builder(INTERVAL_MS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(INTERVAL_MS / 2)
            .build()
        runCatching { engine.requestLocationUpdates(request, callback, Looper.getMainLooper()) }
        runCatching { engine.getLastLocation(callback) }
        awaitClose { runCatching { engine.removeLocationUpdates(callback) } }
    }

    private companion object {
        const val INTERVAL_MS = 2_000L
        const val MAX_AGE_MS = 2 * 60_000L
    }
}
