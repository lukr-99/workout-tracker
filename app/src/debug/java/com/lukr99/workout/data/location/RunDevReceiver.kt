package com.lukr99.workout.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lukr99.workout.WorkoutApp
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * **Debug-only** Run Mode dev/verification helper — the headless, chainable companion to
 * [RunSimReceiver]. Lets an agent seed and inspect Run Mode state from adb without driving the UI:
 *
 * ```
 * # Seed a straight synthetic saved route (name/length/bearing optional):
 * adb shell am broadcast -a com.lukr99.workout.DEV_SEED_ROUTE -n \
 *   com.lukr99.workout/.data.location.RunDevReceiver --es name "Test loop" --ei meters 1200
 *
 * # Dump DB counts to logcat (tag RunDev) for a non-visual assertion:
 * adb shell am broadcast -a com.lukr99.workout.DEV_DUMP -n \
 *   com.lukr99.workout/.data.location.RunDevReceiver
 * adb logcat -d -s RunDev:D | tail -1     # -> RUNMODE_DUMP runs=6 routes=2 linkedRuns=1 points=...
 * ```
 *
 * Ships in the `debug` source set only; never present in release builds.
 */
class RunDevReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val repo = (context.applicationContext as WorkoutApp).container.runRepository
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SEED_ROUTE -> {
                        val name = intent.getStringExtra("name") ?: "Dev route"
                        val lat = intent.getDoubleExtra("lat", 50.0876)
                        val lon = intent.getDoubleExtra("lon", 14.4207)
                        val meters = intent.getIntExtra("meters", 1000).coerceAtLeast(1)
                        val bearing = intent.getDoubleExtra("bearing", 0.0)
                        val route = syntheticRoute(name, lat, lon, meters, bearing)
                        repo.saveRoute(route)
                        android.util.Log.d(TAG, "RUNMODE_SEED_ROUTE id=${route.id} name=$name meters=$meters")
                    }
                    ACTION_DUMP -> {
                        val runs = repo.getRuns()
                        val routes = repo.getRoutes()
                        val linked = runs.count { it.routeId != null }
                        android.util.Log.d(
                            TAG,
                            "RUNMODE_DUMP runs=${runs.size} routes=${routes.size} linkedRuns=$linked",
                        )
                    }
                    ACTION_CLEAR -> {
                        repo.getRuns().forEach { repo.deleteRun(it.id) }
                        repo.getRoutes().forEach { repo.deleteRoute(it.id) }
                        android.util.Log.d(TAG, "RUNMODE_CLEAR done")
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e(TAG, "dev command failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun syntheticRoute(name: String, startLat: Double, startLon: Double, meters: Int, bearingDeg: Double): Route {
        val steps = 24
        val step = meters.toDouble() / steps
        val rad = Math.toRadians(bearingDeg)
        var lat = startLat
        var lon = startLon
        val points = ArrayList<RoutePoint>(steps + 1)
        points += RoutePoint(seq = 0, lat = lat, lon = lon)
        for (i in 1..steps) {
            lat += (step * cos(rad)) / 111_320.0
            lon += (step * sin(rad)) / (111_320.0 * cos(Math.toRadians(lat)))
            points += RoutePoint(seq = i, lat = lat, lon = lon)
        }
        val distance = Pace.pathDistanceMeters(points.map { it.lat to it.lon })
        return Route(
            name = name,
            distanceMeters = distance,
            encodedPolyline = Polyline.encodeRoute(points),
            createdAtUtc = System.currentTimeMillis(),
            points = points,
        )
    }

    private companion object {
        const val TAG = "RunDev"
        const val ACTION_SEED_ROUTE = "com.lukr99.workout.DEV_SEED_ROUTE"
        const val ACTION_DUMP = "com.lukr99.workout.DEV_DUMP"
        const val ACTION_CLEAR = "com.lukr99.workout.DEV_CLEAR"
    }
}
