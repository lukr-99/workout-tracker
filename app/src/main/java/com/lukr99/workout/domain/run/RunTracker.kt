package com.lukr99.workout.domain.run

/**
 * The pure brain of a live run — the running counterpart to strength's live-session reducer. It
 * takes a stream of raw GPS [RunSample]s plus periodic clock [tick]s and derives everything the UI
 * and the saved [Run] need: accepted trace, distance, elapsed vs **moving** time, current/average
 * pace, elevation gain, per-unit [Split]s, and **auto-pause**.
 *
 * Deliberately Android-free and deterministic (all time comes in as parameters), so the full
 * behaviour — jitter/accuracy filtering, moving-time accounting, auto-pause hysteresis, split
 * interpolation — is unit-tested without a device. The [com.lukr99.workout.data.location] service
 * owns an instance, feeds it locations, and persists its [snapshot]; the UI just renders the state.
 *
 * Time model: [RunSample.timeMs] and [tick]/[start]/[pause] take absolute epoch-millis. Elapsed time
 * excludes intervals the runner **manually** paused; moving time additionally excludes **auto-paused**
 * (standing-still) intervals. [TracePoint.t] is stored as an offset from [startedAtUtc].
 */
class RunTracker(private val config: Config = Config()) {

    data class Config(
        /** Reject samples less accurate than this (metres); GPS fixes worse than this are noise. */
        val minAccuracyM: Double = 30.0,
        /** Ignore moves smaller than this between fixes (metres) — stationary GPS wander. */
        val minMoveMeters: Double = 3.0,
        /** At/below this speed (m/s) the run auto-pauses (moving clock stops). ~2.2 km/h. */
        val autoPauseEnterMps: Double = 0.6,
        /** Must exceed this speed (m/s) to leave auto-pause — hysteresis avoids flapping. ~3.2 km/h. */
        val autoPauseExitMps: Double = 0.9,
        val autoPauseEnabled: Boolean = true,
        /** Window for the "current pace" readout — look back at least this far. */
        val currentPaceWindowMeters: Double = 30.0,
        val currentPaceWindowMs: Long = 20_000L,
    )

    enum class Phase { Idle, Recording, Paused, Finished }

    var startedAtUtc: Long = 0L
        private set
    private var phase: Phase = Phase.Idle

    private val points = ArrayList<TracePoint>()
    private var distanceMeters = 0.0
    private var elapsedMs = 0L
    private var movingMs = 0L
    private var autoPaused = false

    private var lastClockMs = 0L
    private var lastAccepted: TracePoint? = null

    /** Begin recording at wall-clock [nowMs]. Resets all state. */
    fun start(nowMs: Long) {
        startedAtUtc = nowMs
        phase = Phase.Recording
        points.clear()
        distanceMeters = 0.0
        elapsedMs = 0L
        movingMs = 0L
        autoPaused = false
        lastClockMs = nowMs
        lastAccepted = null
    }

    /**
     * Advance the elapsed/moving clocks to [nowMs] without a new fix (call ~1 Hz from the service).
     * Elapsed accrues while [Phase.Recording]; moving accrues only when not auto-paused.
     */
    fun tick(nowMs: Long) {
        if (phase != Phase.Recording) {
            lastClockMs = nowMs
            return
        }
        val dt = (nowMs - lastClockMs).coerceAtLeast(0)
        elapsedMs += dt
        if (!autoPaused) movingMs += dt
        lastClockMs = nowMs
    }

    /**
     * Feed a raw GPS fix. Returns true if it was accepted onto the trace (passed the accuracy and
     * min-distance gates). Also advances the clocks to [sample]'s time and updates auto-pause.
     */
    fun onSample(sample: RunSample): Boolean {
        if (phase != Phase.Recording) return false
        tick(sample.timeMs)

        val prev = lastAccepted
        val t = sample.timeMs - startedAtUtc
        val point = TracePoint(
            t = t,
            lat = sample.lat,
            lon = sample.lon,
            elevationM = sample.elevationM,
            speedMps = sample.speedMps,
            accuracyM = sample.accuracyM,
        )

        // The first fix always anchors the run (so a run always has a start point + the map centres),
        // even if the only fix available is a coarse network one. The accuracy gate then rejects
        // poor *subsequent* fixes so GPS jitter can't inflate the distance/trace.
        if (prev == null) {
            points += point
            lastAccepted = point
            return true
        }

        if (sample.accuracyM != null && sample.accuracyM > config.minAccuracyM) return false

        val moved = Pace.haversineMeters(prev.lat, prev.lon, point.lat, point.lon)
        if (moved < config.minMoveMeters) {
            // Standing still (or GPS wander): don't grow distance/trace, but let auto-pause engage.
            updateAutoPause(sample.speedMps ?: 0.0)
            return false
        }

        val dtSec = (point.t - prev.t) / 1000.0
        val segSpeed = if (dtSec > 0) moved / dtSec else (sample.speedMps ?: 0.0)
        updateAutoPause(sample.speedMps ?: segSpeed)

        distanceMeters += moved
        points += point
        lastAccepted = point
        return true
    }

    private fun updateAutoPause(speedMps: Double) {
        if (!config.autoPauseEnabled) {
            autoPaused = false
            return
        }
        autoPaused = if (autoPaused) {
            speedMps < config.autoPauseExitMps // stay paused until clearly moving again
        } else {
            speedMps <= config.autoPauseEnterMps
        }
    }

    /** Manual pause — freezes the elapsed clock too. */
    fun pause(nowMs: Long) {
        tick(nowMs)
        if (phase == Phase.Recording) phase = Phase.Paused
    }

    /** Resume from a manual pause; the paused gap is not counted. */
    fun resume(nowMs: Long) {
        if (phase == Phase.Paused) {
            phase = Phase.Recording
            lastClockMs = nowMs
        }
    }

    /** Finish the run at [nowMs]; the snapshot/[toRun] then reflect final totals. */
    fun finish(nowMs: Long) {
        tick(nowMs)
        phase = Phase.Finished
    }

    val currentPhase: Phase get() = phase

    /** Immutable view of the live state for the UI + notification. Cheap to build each second. */
    fun snapshot(): LiveRunState = LiveRunState(
        phase = phase,
        startedAtUtc = startedAtUtc,
        elapsedSeconds = elapsedMs / 1000,
        movingSeconds = movingMs / 1000,
        distanceMeters = distanceMeters,
        currentPaceSecPerKm = currentPace(),
        avgPaceSecPerKm = Pace.paceSecPerKm(distanceMeters, movingMs / 1000.0),
        elevationGainM = Pace.elevationGainMeters(points),
        autoPaused = autoPaused && phase == Phase.Recording,
        pointCount = points.size,
        lastLat = lastAccepted?.lat,
        lastLon = lastAccepted?.lon,
    )

    /** A copy of the accepted trace (offsets from start). */
    fun trace(): List<TracePoint> = ArrayList(points)

    /** Per-km/mi splits over the trace so far (or final). */
    fun splits(splitMeters: Double = Pace.METERS_PER_KM): List<Split> = Pace.splits(points, splitMeters)

    /** Build the persistable [Run] from the current totals + trace. */
    fun toRun(id: String, sessionId: String? = null): Run = Run(
        id = id,
        sessionId = sessionId,
        startedAtUtc = startedAtUtc,
        durationSeconds = elapsedMs / 1000,
        movingSeconds = movingMs / 1000,
        distanceMeters = distanceMeters,
        avgPaceSecPerKm = Pace.paceSecPerKm(distanceMeters, movingMs / 1000.0),
        elevationGainM = Pace.elevationGainMeters(points),
        source = RunSource.Local,
        trace = trace(),
    )

    /** Rolling current pace over the recent window; `0.0` until enough recent movement. */
    private fun currentPace(): Double {
        if (points.size < 2) return 0.0
        val last = points.last()
        var dist = 0.0
        var i = points.lastIndex
        while (i > 0) {
            val a = points[i - 1]
            val b = points[i]
            dist += Pace.haversineMeters(a.lat, a.lon, b.lat, b.lon)
            val spanMs = last.t - a.t
            if (dist >= config.currentPaceWindowMeters || spanMs >= config.currentPaceWindowMs) {
                return Pace.paceSecPerKm(dist, spanMs / 1000.0)
            }
            i--
        }
        val spanMs = last.t - points.first().t
        return Pace.paceSecPerKm(dist, spanMs / 1000.0)
    }
}

/** One raw GPS fix handed to [RunTracker] (absolute epoch-millis [timeMs]). */
data class RunSample(
    val timeMs: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Double? = null,
    val speedMps: Double? = null,
    val elevationM: Double? = null,
)

/** Immutable live-run readout derived by [RunTracker], observed by the UI and notification. */
data class LiveRunState(
    val phase: RunTracker.Phase = RunTracker.Phase.Idle,
    val startedAtUtc: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val movingSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val autoPaused: Boolean = false,
    val pointCount: Int = 0,
    val lastLat: Double? = null,
    val lastLon: Double? = null,
) {
    val isActive: Boolean get() = phase == RunTracker.Phase.Recording || phase == RunTracker.Phase.Paused
}
