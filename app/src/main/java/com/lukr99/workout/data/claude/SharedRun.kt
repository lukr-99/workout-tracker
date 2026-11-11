package com.lukr99.workout.data.claude

import com.lukr99.workout.domain.InstantMillisSerializer
import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.Split
import kotlinx.serialization.Serializable

/** A run as Claude gets it: the summary and its kilometre splits, never the GPS trace. */
@Serializable
data class SharedRun(
    val id: String,
    @Serializable(with = InstantMillisSerializer::class)
    val startedAtUtc: Long,
    val durationSeconds: Long,
    val movingSeconds: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double,
    val elevationGainM: Double,
    val calories: Double? = null,
    val avgHr: Int? = null,
    val routeId: String? = null,
    val notes: String = "",
    val splits: List<Split> = emptyList(),
) {
    companion object {
        /** [run] with the splits of its trace; a run without a trace (an import) has none. */
        fun of(run: Run): SharedRun = SharedRun(
            id = run.id,
            startedAtUtc = run.startedAtUtc,
            durationSeconds = run.durationSeconds,
            movingSeconds = run.movingSeconds,
            distanceMeters = run.distanceMeters,
            avgPaceSecPerKm = run.avgPaceSecPerKm,
            elevationGainM = run.elevationGainM,
            calories = run.calories,
            avgHr = run.avgHr,
            routeId = run.routeId,
            notes = run.notes,
            splits = if (run.trace.size < 2) emptyList() else Pace.splits(run.trace),
        )
    }
}
