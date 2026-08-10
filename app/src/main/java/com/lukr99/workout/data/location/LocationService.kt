package com.lukr99.workout.data.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.lukr99.workout.MainActivity
import com.lukr99.workout.R
import com.lukr99.workout.WorkoutApp
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.RunTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * Foreground service that keeps GPS sampling with the screen off during a live run. It is a thin
 * Android host around [RunSessionController]: it streams high-accuracy fixes from MapLibre's
 * [LocationEngine] (fused where Play Services is present — no extra dependency) into the controller,
 * ticks its clocks ~1 Hz, and shows an ongoing notification with distance/time/pace + pause/resume/
 * stop actions. All run state, math, and persistence live in the controller; this class owns only
 * the OS-level concerns (foreground promotion, location updates, notification).
 *
 * `foregroundServiceType="location"` (manifest) is what lets sampling continue backgrounded, so no
 * `ACCESS_BACKGROUND_LOCATION` is needed.
 */
class LocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var watcherJob: Job? = null

    private lateinit var controller: RunSessionController
    private lateinit var notifications: NotificationManager
    private var engine: LocationEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult) {
            val loc = result.lastLocation ?: return
            // Use one wall clock (the controller's default) for all time bookkeeping so elapsed/moving
            // never jump from GPS-vs-system clock skew; the GPS fix supplies only position/accuracy.
            controller.onLocation(
                lat = loc.latitude,
                lon = loc.longitude,
                accuracyM = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
                speedMps = if (loc.hasSpeed()) loc.speed.toDouble() else null,
                elevationM = if (loc.hasAltitude()) loc.altitude else null,
            )
            refreshNotification(controller.state.value)
        }

        override fun onFailure(exception: Exception) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        controller = (application as WorkoutApp).container.runSessionController
        notifications = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> onStart()
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_STOP -> onStop()
        }
        return START_STICKY
    }

    private fun onStart() {
        // Promote to foreground immediately (must happen within a few seconds of startForegroundService).
        startForegroundWith(controller.state.value)
        controller.start()
        acquireWakeLock()
        startLocationUpdates()

        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                controller.tick()
                refreshNotification(controller.state.value)
            }
        }
        // Stop the service once the run is finished/idle (e.g. finished from the UI).
        watcherJob?.cancel()
        watcherJob = scope.launch {
            controller.state.collect { state ->
                if (state.phase == RunTracker.Phase.Finished || state.phase == RunTracker.Phase.Idle) {
                    tearDown()
                }
            }
        }
    }

    /** Notification STOP: finish + persist through the controller, then tear the service down. */
    private fun onStop() {
        scope.launch {
            controller.finish()
            tearDown()
        }
    }

    private fun tearDown() {
        stopLocationUpdates()
        releaseWakeLock()
        tickerJob?.cancel()
        watcherJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Hold a partial wake lock for the life of the run. The `location` foreground-service type keeps
     * us *allowed* to sample with the screen off, but on a dozing device the CPU can still suspend
     * between fixes — starving both the location callbacks and the 1 Hz ticker, which is what makes a
     * backgrounded run record in sparse bursts with straight lines across the gaps. Keeping the CPU
     * awake while recording gives a continuous, live trace. A generous safety timeout guarantees the
     * lock can never outlive a run even if teardown is somehow missed.
     */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            runCatching { acquire(WAKE_LOCK_TIMEOUT_MS) }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock -> runCatching { if (lock.isHeld) lock.release() } }
        wakeLock = null
    }

    @Suppress("MissingPermission") // Started only after ACCESS_FINE_LOCATION is granted.
    private fun startLocationUpdates() {
        if (engine != null) return
        val locationEngine = LocationEngineDefault.getDefaultLocationEngine(applicationContext)
        val request = LocationEngineRequest.Builder(UPDATE_INTERVAL_MS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(FASTEST_INTERVAL_MS)
            .build()
        runCatching {
            locationEngine.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
        // Seed immediately with the last known fix so the run has a start anchor (and the map
        // centres) even before the first live update arrives — important indoors / cold GPS.
        runCatching { locationEngine.getLastLocation(locationCallback) }
        engine = locationEngine
    }

    private fun stopLocationUpdates() {
        runCatching { engine?.removeLocationUpdates(locationCallback) }
        engine = null
    }

    override fun onDestroy() {
        stopLocationUpdates()
        releaseWakeLock()
        tickerJob?.cancel()
        watcherJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Notification --------------------------------------------------------------------------

    private fun startForegroundWith(state: LiveRunState) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(state), type)
    }

    private fun refreshNotification(state: LiveRunState) {
        if (!state.isActive) return
        notifications.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: LiveRunState): Notification {
        val paused = state.phase == RunTracker.Phase.Paused
        val distanceKm = "%.2f km".format(state.distanceMeters / Pace.METERS_PER_KM)
        val time = Pace.formatDuration(state.movingSeconds)
        val pace = Pace.formatPace(state.avgPaceSecPerKm)
        val title = if (paused) "Run paused" else "Recording run"
        val text = "$distanceKm · $time · $pace /km"

        val content = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_run)
            .setContentIntent(content)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (paused) {
            builder.addAction(0, "Resume", action(ACTION_RESUME))
        } else {
            builder.addAction(0, "Pause", action(ACTION_PAUSE))
        }
        builder.addAction(0, "Finish", action(ACTION_STOP))
        return builder.build()
    }

    private fun action(name: String): PendingIntent {
        val intent = Intent(this, LocationService::class.java).setAction(name)
        return PendingIntent.getService(
            this, name.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Run tracking", NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Ongoing notification while a run is being recorded."
                setShowBadge(false)
            }
            notifications.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.lukr99.workout.run.START"
        const val ACTION_PAUSE = "com.lukr99.workout.run.PAUSE"
        const val ACTION_RESUME = "com.lukr99.workout.run.RESUME"
        const val ACTION_STOP = "com.lukr99.workout.run.STOP"

        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 4201
        private const val UPDATE_INTERVAL_MS = 1_000L
        private const val FASTEST_INTERVAL_MS = 1_000L
        private const val WAKE_LOCK_TAG = "ember:run-tracking"
        // Safety net only — a run should never run this long; the lock is released on finish/stop.
        private const val WAKE_LOCK_TIMEOUT_MS = 6L * 60L * 60L * 1_000L

        fun start(context: Context) = send(context, ACTION_START)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            val intent = Intent(context, LocationService::class.java).setAction(action)
            if (action == ACTION_START && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
