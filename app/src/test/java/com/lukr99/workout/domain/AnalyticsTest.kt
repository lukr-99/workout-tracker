package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Analytics parity with the MAUI repository, over handcrafted history and a fixed `now` so the
 * time-relative maths (30-day window, weekly streak) is deterministic.
 */
class AnalyticsTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L // fixed reference "now"

    private fun strengthEntry(name: String, bodyPart: String, vararg sets: Pair<Int, Double>) =
        WorkoutEntry(
            exerciseId = name,
            exerciseSnapshotName = name,
            exerciseSnapshotCategory = ExerciseCategory.Strength,
            exerciseSnapshotPrimaryBodyPart = bodyPart,
            entryType = ExerciseCategory.Strength,
            strengthSets = sets.mapIndexed { i, (reps, kg) ->
                StrengthSet(setNumber = i + 1, reps = reps, weightKg = kg)
            },
        )

    private fun completed(name: String, completedAt: Long, vararg entries: WorkoutEntry) =
        WorkoutSession(
            name = name,
            startedAtUtc = completedAt,
            completedDateUtc = completedAt,
            status = WorkoutSessionStatus.Completed,
            entries = entries.toList(),
        )

    @Test
    fun summarize_countsSetsVolumeAndBodyParts() {
        val session = completed(
            "Push", now,
            strengthEntry("Bench", "Chest", 5 to 100.0, 5 to 100.0),
            strengthEntry("OHP", "Shoulders", 8 to 40.0),
        )

        val summary = Analytics.summarize(session)

        assertEquals(2, summary.exerciseCount)
        assertEquals(3, summary.strengthSetCount)
        assertEquals(100.0 * 5 + 100.0 * 5 + 40.0 * 8, summary.totalVolumeKg, 1e-9)
        assertEquals("Strength", summary.sessionTypeLabel)
        assertEquals("Chest • Shoulders", summary.bodyPartsSummary)
    }

    @Test
    fun summarize_mixedSessionAndCardioMinutes() {
        val session = completed(
            "Mixed", now,
            strengthEntry("Squat", "Legs", 5 to 140.0),
            WorkoutEntry(
                exerciseSnapshotName = "Row Erg",
                exerciseSnapshotCategory = ExerciseCategory.Cardio,
                exerciseSnapshotPrimaryBodyPart = "Cardio",
                entryType = ExerciseCategory.Cardio,
                cardioData = CardioEntryData(durationSeconds = 20 * 60),
            ),
        )

        val summary = Analytics.summarize(session)
        assertEquals("Mixed", summary.sessionTypeLabel)
        assertEquals(20, summary.cardioMinutes)
    }

    @Test
    fun overview_windowsVolumeAndMostLogged() {
        val recent = completed("A", now - 2 * day, strengthEntry("Bench", "Chest", 5 to 100.0))
        val alsoRecent = completed("B", now - 5 * day, strengthEntry("Bench", "Chest", 5 to 100.0))
        val old = completed("C", now - 60 * day, strengthEntry("Squat", "Legs", 5 to 140.0))

        val overview = Analytics.overview(listOf(recent, alsoRecent, old), nowUtcMillis = now)

        assertEquals(3, overview.totalCompletedWorkouts)
        assertEquals(2, overview.workoutsLast30Days)
        assertEquals(100.0 * 5 * 2 + 140.0 * 5, overview.totalVolumeKg, 1e-9)
        assertEquals("Bench", overview.mostLoggedExerciseName) // logged in 2 sessions vs Squat 1
    }

    @Test
    fun consistency_countsDaysAndLongestGap() {
        val dates = listOf(now, now - 3 * day, now - 20 * day, now - 50 * day)
        val snapshot = Analytics.consistency(dates, nowUtcMillis = now)

        assertEquals(2, snapshot.workoutsLast7Days)   // now, now-3d
        assertEquals(3, snapshot.workoutsLast30Days)  // now, now-3d, now-20d
        assertEquals(30, snapshot.longestGapDays)     // 50d -> 20d is the biggest gap
    }

    @Test
    fun weeklyStreak_countsConsecutiveIsoWeeks() {
        val consecutive = listOf(now, now - 7 * day, now - 14 * day)
        assertEquals(3, Analytics.weeklyStreak(consecutive, nowUtcMillis = now))

        val broken = listOf(now, now - 21 * day) // skips the previous two weeks
        assertEquals(1, Analytics.weeklyStreak(broken, nowUtcMillis = now))

        assertEquals(0, Analytics.weeklyStreak(emptyList(), nowUtcMillis = now))
    }
}
