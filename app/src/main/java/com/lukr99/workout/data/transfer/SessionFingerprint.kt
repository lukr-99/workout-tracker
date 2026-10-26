package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.effectiveTags
import com.lukr99.workout.domain.formatFixed

object SessionFingerprint {
    fun identity(session: WorkoutSession): String =
        "${session.name.normalize()}|${session.startedAtUtc / 60_000}"

    fun of(session: WorkoutSession): String {
        val raw = buildString {
            append(identity(session)).append('|').append(session.durationSeconds)
            session.entries.sortedBy(WorkoutEntry::sortOrder).forEach { entry ->
                append('|').append(entry.exerciseSnapshotName.normalize())
                append(':').append(entry.entryType.ordinal)
                entry.strengthSets.sortedBy(StrengthSet::setNumber).forEach { set ->
                    append(':').append(set.reps)
                    append('@').append(formatFixed(set.weightKg, 6))
                    append('/').append(set.durationSeconds ?: 0)
                    append('/').append(set.setType.ordinal)
                    append('/').append(set.effectiveTags.sortedBy { it.ordinal }.joinToString(".") { it.ordinal.toString() })
                }
                entry.cardioData?.let {
                    append(":c").append(it.durationSeconds)
                    append('/').append(formatFixed(it.distanceKm ?: 0.0, 6))
                }
            }
        }
        return Sha256.hexOf(raw)
    }

    private fun String.normalize(): String = lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
}
