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
 * first good enough fix, and unregisters, so nothing keeps listening. No background location is
 * involved. Only precise location counts: an approximate fix is blurred by about 2 km, which is
 * useless for a 150 m match.
 *
 * [FixQuality.Balanced] (workout start) also takes a last-known fix younger than [MAX_AGE_MS], since
 * a gym does not move, but only one at least as accurate as [BALANCED_MAX_ACCURACY_M].
 * [FixQuality.High] (saving a place) ignores last-known fixes, stops early on a fix within
 * [HIGH_GOOD_ACCURACY_M], and otherwise keeps the best fresh fix within [HIGH_MAX_ACCURACY_M].
 */
class EnginePositionProvider(private val context: Context) : PositionProvider {

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Checked by hasPermission() first.
    override suspend fun currentPosition(quality: FixQuality, timeoutMs: Long): GeoPoint? {
        if (!hasPermission()) return null
        val engine = LocationEngineDefault.getDefaultLocationEngine(context.applicationContext)
        val maxAccuracy = if (quality == FixQuality.High) HIGH_MAX_ACCURACY_M else BALANCED_MAX_ACCURACY_M
        val goodAccuracy = if (quality == FixQuality.High) HIGH_GOOD_ACCURACY_M else BALANCED_MAX_ACCURACY_M
        val startedAt = System.currentTimeMillis()
        var best: android.location.Location? = null
        var callback: LocationEngineCallback<LocationEngineResult>? = null
        return try {
            val good = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    val cb = object : LocationEngineCallback<LocationEngineResult> {
                        override fun onSuccess(result: LocationEngineResult?) {
                            val fix = result?.lastLocation ?: return
                            val oldest = if (quality == FixQuality.High) startedAt else System.currentTimeMillis() - MAX_AGE_MS
                            if (fix.time < oldest || !fix.hasAccuracy() || fix.accuracy > maxAccuracy) return
                            if (best.let { it == null || fix.accuracy < it.accuracy }) best = fix
                            if (fix.accuracy <= goodAccuracy && cont.isActive) cont.resume(fix)
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
                    if (quality == FixQuality.Balanced) runCatching { engine.getLastLocation(cb) }
                }
            }
            (good ?: best)?.let { GeoPoint(it.latitude, it.longitude) }
        } finally {
            callback?.let { cb -> runCatching { engine.removeLocationUpdates(cb) } }
        }
    }

    private companion object {
        const val INTERVAL_MS = 1_000L
        const val MAX_AGE_MS = 2 * 60_000L
        const val BALANCED_MAX_ACCURACY_M = 150f
        const val HIGH_GOOD_ACCURACY_M = 25f
        const val HIGH_MAX_ACCURACY_M = 100f
    }
}
