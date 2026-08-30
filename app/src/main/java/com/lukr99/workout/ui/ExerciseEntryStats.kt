package com.lukr99.workout.ui

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.components.Format

/** Compact per-exercise facts shared by the live collapsed card and completed-workout detail. */
data class ExerciseEntryStats(
    val sets: Int,
    val reps: Int,
    val volumeKg: Double,
    val bestSet: StrengthSet?,
    val durationSeconds: Long?,
)

fun WorkoutEntry.stats(
    nowUtcMillis: Long = System.currentTimeMillis(),
    includeUnperformed: Boolean = false,
): ExerciseEntryStats {
    val performed = strengthSets.filter { it.performedAtUtc != null }
    // Imported and older completed entries did not record performedAtUtc, so history falls back to
    // all sets. A currently running exercise only counts sets that were actually checked off.
    val countedSets = when {
        performed.isNotEmpty() -> performed
        includeUnperformed -> strengthSets
        else -> emptyList()
    }
    val duration = startedAtUtc?.let { started ->
        ((completedAtUtc ?: nowUtcMillis) - started).coerceAtLeast(0L) / 1_000L
    }
    return ExerciseEntryStats(
        sets = countedSets.size,
        reps = countedSets.sumOf(StrengthSet::reps),
        volumeKg = Estimates.volume(countedSets),
        bestSet = countedSets.maxByOrNull { it.weightKg * it.reps },
        durationSeconds = duration,
    )
}

fun WorkoutEntry.statsSummary(
    units: UnitSystem,
    nowUtcMillis: Long = System.currentTimeMillis(),
    includeUnperformed: Boolean = false,
): String {
    val stats = stats(nowUtcMillis, includeUnperformed)
    return if (isStrength) {
        buildList {
            add("${stats.sets} sets")
            add("${stats.reps} reps")
            add("${Format.volume(stats.volumeKg, units)} ${Format.unitLabel(units)}")
            stats.durationSeconds?.let { add(Format.duration(it)) }
        }.joinToString("  •  ")
    } else {
        buildList {
            cardioData?.durationSeconds?.takeIf { it > 0 }?.let { add(Format.duration(it.toLong())) }
            cardioData?.distanceKm?.let { add(Format.distance(it * 1_000.0, units)) }
            cardioData?.calories?.let { add("${Math.round(it)} kcal") }
            stats.durationSeconds?.let { elapsed ->
                val loggedDuration = cardioData?.durationSeconds?.toLong()
                if (loggedDuration == null || elapsed != loggedDuration) add("${Format.duration(elapsed)} elapsed")
            }
            if (isEmpty()) add("No cardio stats")
        }.joinToString("  •  ")
    }
}
