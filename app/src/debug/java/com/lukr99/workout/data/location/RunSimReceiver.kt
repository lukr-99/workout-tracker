package com.lukr99.workout.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lukr99.workout.WorkoutApp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * **Debug-only** GPS route simulator — the repeatable way to exercise a live run without going
 * outside or driving the UI by hand. It feeds a synthetic straight-line route straight into
 * [RunSessionController] (no real GPS / service), so distance, pace, splits, the growing map polyline,
 * and persistence can be tested from one adb command:
 *
 * ```
 * adb shell am broadcast -a com.lukr99.workout.SIM_RUN \
 *   --ed lat 50.0876 --ed lon 14.4207 --ei meters 2000 --ei seconds 600 --ed bearing 90
 * ```
 *
 * Simulated time comes from synthetic per-step timestamps (so the saved run has the right duration and
 * pace), while the real delay between steps is tiny — a 10-minute run replays in a few seconds. Open
 * the Run screen first to watch the ember polyline grow, or just check the Runs hub afterwards. Ships
 * in the `debug` source set only; it is not present in release builds.
 */
class RunSimReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WorkoutApp).container
        val controller = container.runSessionController
        val lat = intent.getDoubleExtra("lat", 50.0876)
        val lon = intent.getDoubleExtra("lon", 14.4207)
        val meters = intent.getIntExtra("meters", 1000).coerceAtLeast(1)
        val seconds = intent.getIntExtra("seconds", 300).coerceAtLeast(1)
        val bearingDeg = intent.getDoubleExtra("bearing", 0.0)
        val useRoute = intent.getBooleanExtra("useRoute", false)

        android.util.Log.d(TAG, "onReceive meters=$meters seconds=$seconds bearing=$bearingDeg useRoute=$useRoute")
        val pending = goAsync()
        scope.launch {
            try {
                val routeId = if (useRoute) container.runRepository.getRoutes().firstOrNull()?.id else null
                val run = simulate(controller, lat, lon, meters, seconds, bearingDeg, routeId)
                android.util.Log.d(TAG, "sim finished dist=${run.distanceMeters} points=${run.trace.size}")
            } catch (t: Throwable) {
                android.util.Log.e(TAG, "sim failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun simulate(
        controller: RunSessionController,
        startLat: Double,
        startLon: Double,
        meters: Int,
        seconds: Int,
        bearingDeg: Double,
        routeId: String?,
    ): com.lukr99.workout.domain.run.Run {
        controller.discard() // clear any prior/finished state
        controller.start()
        controller.armRoute(routeId) // link this run to a saved route (verification of start-from-route)

        val stepMeters = meters.toDouble() / STEPS
        val stepMs = (seconds * 1000L) / STEPS
        val speed = meters.toDouble() / seconds
        val bearing = Math.toRadians(bearingDeg)
        val base = System.currentTimeMillis()

        var lat = startLat
        var lon = startLon
        controller.onLocation(lat, lon, accuracyM = 5.0, speedMps = speed, elevationM = 200.0, timeMs = base)
        for (i in 1..STEPS) {
            lat += (stepMeters * cos(bearing)) / 111_320.0
            lon += (stepMeters * sin(bearing)) / (111_320.0 * cos(Math.toRadians(lat)))
            controller.onLocation(
                lat, lon, accuracyM = 5.0, speedMps = speed, elevationM = 200.0,
                timeMs = base + i * stepMs,
            )
            delay(REPLAY_STEP_MS)
        }
        return controller.finish()
    }

    private companion object {
        const val TAG = "RunSim"
        const val STEPS = 120
        const val REPLAY_STEP_MS = 40L
    }
}
