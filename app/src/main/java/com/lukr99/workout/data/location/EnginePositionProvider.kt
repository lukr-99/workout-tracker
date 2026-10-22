package com.lukr99.workout.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.lukr99.workout.domain.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import kotlin.coroutines.resume

/**
 * One-shot fix from the same location engine the run mode uses. It registers for updates, takes the
 * first fix, and unregisters, so nothing keeps listening. A last-known fix younger than
 * [MAX_AGE_MS] counts, since a gym does not move. No background location is involved.
 */
class EnginePositionProvider(private val context: Context) : PositionProvider {

    override fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission") // Checked by hasPermission() first.
    override suspend fun currentPosition(quality: FixQuality, timeoutMs: Long): GeoPoint? {
        if (!hasPermission()) return null
        val engine = LocationEngineDefault.getDefaultLocationEngine(context.applicationContext)
        var callback: LocationEngineCallback<LocationEngineResult>? = null
        return try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    val cb = object : LocationEngineCallback<LocationEngineResult> {
                        override fun onSuccess(result: LocationEngineResult?) {
                            val fix = result?.lastLocation ?: return
                            if (System.currentTimeMillis() - fix.time > MAX_AGE_MS) return
                            if (cont.isActive) cont.resume(GeoPoint(fix.latitude, fix.longitude))
                        }

                        override fun onFailure(exception: Exception) = Unit
                    }
                    callback = cb
                    val priority = when (quality) {
                        FixQuality.Balanced -> LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY
                        FixQuality.High -> LocationEngineRequest.PRIORITY_HIGH_ACCURACY
                    }
                    val request = LocationEngineRequest.Builder(INTERVAL_MS).setPriority(priority).setMaxWaitTime(timeoutMs).build()
                    runCatching { engine.requestLocationUpdates(request, cb, Looper.getMainLooper()) }
                    runCatching { engine.getLastLocation(cb) }
                }
            }
        } finally {
            callback?.let { cb -> runCatching { engine.removeLocationUpdates(cb) } }
        }
    }

    private companion object {
        const val INTERVAL_MS = 1_000L
        const val MAX_AGE_MS = 2 * 60_000L
    }
}
