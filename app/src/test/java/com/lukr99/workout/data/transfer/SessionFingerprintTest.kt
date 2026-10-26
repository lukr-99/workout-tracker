package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.effectiveTags
import java.security.MessageDigest
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFingerprintTest {
    /** The fingerprint as it was computed before the pure Kotlin SHA-256 and number formatting. */
    private fun legacy(session: WorkoutSession): String {
        val raw = buildString {
            append("${session.name.lowercase().replace(Regex("\\s+"), " ").trim()}|${session.startedAtUtc / 60_000}")
            append('|').append(session.durationSeconds)
            session.entries.sortedBy(WorkoutEntry::sortOrder).forEach { entry ->
                append('|').append(entry.exerciseSnapshotName.lowercase().replace(Regex("\\s+"), " ").trim())
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

    private fun session(index: Int): WorkoutSession {
        val entryId = "entry$index"
        return WorkoutSession(
            id = "s$index",
            name = "  Push   DAY $index  ",
            startedAtUtc = 1_704_103_200_000L + index * 61_234L,
            durationSeconds = 3_000L + index,
            entries = listOf(
                WorkoutEntry(
                    id = entryId,
                    workoutSessionId = "s$index",
                    exerciseSnapshotName = "Bench Press $index",
                    entryType = ExerciseCategory.Strength,
                    strengthSets = (1..3).map { n ->
                        StrengthSet(
                            workoutEntryId = entryId,
                            setNumber = n,
                            reps = 5 + n,
                            weightKg = 62.5 + index * 0.1234567 + n / 3.0,
                            tags = if (n == 2) setOf(SetTag.ToFailure, SetTag.Failed) else emptySet(),
                        )
                    },
                ),
                WorkoutEntry(
                    id = "run$index",
                    workoutSessionId = "s$index",
                    exerciseSnapshotName = "Run",
                    entryType = ExerciseCategory.Cardio,
                    sortOrder = 1,
                    cardioData = CardioEntryData(
                        workoutEntryId = "run$index",
                        durationSeconds = 1_800,
                        distanceKm = 5.0 + index / 7.0,
                    ),
                ),
            ),
        )
    }

    @Test
    fun fingerprintsMatchTheOldComputation() {
        for (index in 0 until 40) {
            val session = session(index)
            assertEquals("session $index", legacy(session), SessionFingerprint.of(session))
        }
    }
}
