package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.effectiveTags
import java.security.MessageDigest
import java.util.Locale

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
                    append('@').append(String.format(Locale.ROOT, "%.6f", set.weightKg))
                    append('/').append(set.durationSeconds ?: 0)
                    append('/').append(set.setType.ordinal)
                    append('/').append(set.effectiveTags.sortedBy { it.ordinal }.joinToString(".") { it.ordinal.toString() })
                }
                entry.cardioData?.let {
                    append(":c").append(it.durationSeconds)
                    append('/').append(String.format(Locale.ROOT, "%.6f", it.distanceKm ?: 0.0))
                }
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun String.normalize(): String = lowercase()
        .replace(Regex("\\s+"), " ")
        .trim()
}
