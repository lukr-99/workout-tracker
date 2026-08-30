package com.lukr99.workout.ui

import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.effectiveTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutEntryStatsTest {
    @Test
    fun legacyFailureMeansToFailureWhileNewTagsCanAlsoRecordFailedEarly() {
        assertEquals(setOf(SetTag.ToFailure), StrengthSet(setType = SetType.Failure).effectiveTags)
        assertEquals(setOf(SetTag.Warmup), StrengthSet(isWarmup = true).effectiveTags)

        val combined = StrengthSet(
            setType = SetType.Failure,
            tags = setOf(SetTag.ToFailure, SetTag.Failed),
        )
        assertEquals(setOf(SetTag.ToFailure, SetTag.Failed), combined.effectiveTags)
    }

    @Test
    fun liveStatsCountOnlyPerformedSetsAndTrackExerciseElapsedTime() {
        val entry = WorkoutEntry(
            startedAtUtc = 1_000,
            strengthSets = listOf(
                StrengthSet(reps = 5, weightKg = 100.0, performedAtUtc = 2_000),
                StrengthSet(reps = 8, weightKg = 80.0),
            ),
        )

        val stats = entry.stats(nowUtcMillis = 61_000)

        assertEquals(1, stats.sets)
        assertEquals(5, stats.reps)
        assertEquals(500.0, stats.volumeKg, 0.001)
        assertEquals(60L, stats.durationSeconds)
    }

    @Test
    fun notStartedEntryHasNoElapsedTimeOrPlannedVolume() {
        val stats = WorkoutEntry(
            strengthSets = listOf(StrengthSet(reps = 5, weightKg = 100.0)),
        ).stats()

        assertEquals(0, stats.sets)
        assertEquals(0.0, stats.volumeKg, 0.001)
        assertNull(stats.durationSeconds)
    }
}
