package com.lukr99.workout.data.location

import android.content.Context
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.newId
import com.lukr99.workout.domain.run.LiveRunState
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.RunSample
import com.lukr99.workout.domain.run.RunSource
import com.lukr99.workout.domain.run.RunTracker
import com.lukr99.workout.domain.run.TracePoint
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Process-wide brain + persistence for the single active run — the one place the app's live-run
 * state lives. The [LocationService] feeds it GPS fixes and clock ticks; the
 * [com.lukr99.workout.ui.run.LiveRunViewModel] observes [state]/[trace] and drives start/pause/
 * resume/finish. It owns a pure [RunTracker] (all the math) and an on-disk **crash buffer** so a run
 * survives process death: every accepted fix is flushed to disk, [recoverIfNeeded] salvages a
 * leftover buffer into a saved run on next launch, and [finish] clears it.
 *
 * Held once by [com.lukr99.workout.data.AppContainer]; the service reaches it via the Application.
 */
class RunSessionController(
    private val appContext: Context,
    private val repository: RunRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val tracker = RunTracker()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bufferMutex = Mutex()
    private val bufferFile = File(appContext.filesDir, BUFFER_FILE)
    private val json = Json { ignoreUnknownKeys = true }

    private var runId: String = ""
    private var lastFinished: Run? = null

    private val _state = MutableStateFlow(LiveRunState())
    val state: StateFlow<LiveRunState> = _state.asStateFlow()

    private val _trace = MutableStateFlow<List<TracePoint>>(emptyList())
    val trace: StateFlow<List<TracePoint>> = _trace.asStateFlow()

    /** Whether a run is currently recording or paused (used to gate re-entry / notification). */
    val isRunning: Boolean get() = _state.value.isActive

    fun start() {
        if (isRunning) return
        runId = newId()
        lastFinished = null
        tracker.start(clock())
        publish()
        flushBuffer()
    }

    /** A raw fix from the location engine. [timeMs] is absolute epoch-millis. */
    fun onLocation(
        lat: Double,
        lon: Double,
        accuracyM: Double?,
        speedMps: Double?,
        elevationM: Double?,
        timeMs: Long = clock(),
    ) {
        val accepted = tracker.onSample(RunSample(timeMs, lat, lon, accuracyM, speedMps, elevationM))
        publish()
        if (accepted) flushBuffer()
    }

    /** Advance the elapsed/moving clocks (~1 Hz from the service). */
    fun tick() {
        tracker.tick(clock())
        publish()
    }

    fun pause() {
        tracker.pause(clock())
        publish()
    }

    fun resume() {
        tracker.resume(clock())
        publish()
    }

    /**
     * Finish + persist the run, returning it. Idempotent: a second call (e.g. notification STOP racing
     * the on-screen Finish) returns the already-saved run without saving twice.
     */
    suspend fun finish(): Run {
        lastFinished?.let { return it }
        tracker.finish(clock())
        val run = tracker.toRun(runId.ifEmpty { newId() })
        if (run.trace.isNotEmpty()) repository.saveRun(run)
        lastFinished = run
        clearBuffer()
        publish()
        return run
    }

    /** Abandon the active run without saving (and clear the buffer). */
    fun discard() {
        lastFinished = null
        runId = ""
        clearBuffer()
        tracker.finish(clock())
        _state.value = LiveRunState()
        _trace.value = emptyList()
    }

    /**
     * On app start: if a crash buffer survived a killed process, rebuild a [Run] from its trace and
     * save it so the run isn't lost, then clear the buffer. No-op when there's nothing to recover.
     */
    suspend fun recoverIfNeeded(): Run? = bufferMutex.withLock {
        if (isRunning) return null
        val buffer = readBuffer() ?: return null
        if (buffer.trace.size < 2) {
            deleteBufferFile()
            return null
        }
        val distance = Pace.traceDistanceMeters(buffer.trace)
        val durationSec = (buffer.trace.last().t / 1000)
        val run = Run(
            id = buffer.runId.ifEmpty { newId() },
            startedAtUtc = buffer.startedAtUtc,
            durationSeconds = durationSec,
            movingSeconds = durationSec,
            distanceMeters = distance,
            avgPaceSecPerKm = Pace.paceSecPerKm(distance, durationSec.toDouble()),
            elevationGainM = Pace.elevationGainMeters(buffer.trace),
            source = RunSource.Local,
            notes = "Recovered after the app closed mid-run.",
            trace = buffer.trace,
        )
        repository.saveRun(run)
        deleteBufferFile()
        run
    }

    private fun publish() {
        _state.value = tracker.snapshot()
        _trace.value = tracker.trace()
    }

    // --- Crash buffer --------------------------------------------------------------------------

    private fun flushBuffer() {
        val snapshot = RunBuffer(runId, tracker.startedAtUtc, tracker.trace())
        scope.launch {
            bufferMutex.withLock {
                runCatching { bufferFile.writeText(json.encodeToString(RunBuffer.serializer(), snapshot)) }
            }
        }
    }

    private fun clearBuffer() {
        scope.launch { bufferMutex.withLock { deleteBufferFile() } }
    }

    private fun deleteBufferFile() {
        runCatching { if (bufferFile.exists()) bufferFile.delete() }
    }

    private fun readBuffer(): RunBuffer? = runCatching {
        if (!bufferFile.exists()) return null
        json.decodeFromString(RunBuffer.serializer(), bufferFile.readText())
    }.getOrNull()

    @Serializable
    private data class RunBuffer(
        val runId: String,
        val startedAtUtc: Long,
        val trace: List<TracePoint>,
    )

    companion object {
        private const val BUFFER_FILE = "run_crash_buffer.json"
    }
}
