package com.lukr99.workout.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.IsoFields

/**
 * Consistency / overview analytics, ported as **pure functions** from the MAUI
 * `WorkoutTrackerRepository` (`GetWorkoutHistoryAsync`, `GetAnalyticsOverviewAsync`,
 * `GetConsistencySnapshotAsync`). No Android imports — everything is derived from the domain models
 * and an injectable `nowUtcMillis`, so it is deterministic under unit test.
 *
 * "Store raw, derive stats": nothing here is persisted; it is recomputed from history.
 */
object Analytics {

    /** Per-session rollup — mirrors the `WorkoutSessionSummary` projection MAUI built in-memory. */
    fun summarize(session: WorkoutSession): WorkoutSessionSummary {
        val strengthSets = session.entries.flatMap { it.strengthSets }
        return WorkoutSessionSummary(
            id = session.id,
            name = session.name,
            startedAtUtc = session.startedAtUtc,
            completedDateUtc = session.completedDateUtc,
            status = session.status,
            exerciseCount = session.entries.size,
            strengthSetCount = strengthSets.size,
            totalVolumeKg = strengthSets.sumOf { it.weightKg * it.reps },
            cardioMinutes = session.entries.sumOf { (it.cardioData?.durationSeconds ?: 0) / 60 },
            sessionTypeLabel = describeSessionType(session.entries),
            bodyPartsSummary = describeBodyParts(session.entries),
        )
    }

    /** Dashboard overview across completed sessions. */
    fun overview(
        completed: List<WorkoutSession>,
        nowUtcMillis: Long = System.currentTimeMillis(),
    ): AnalyticsOverview {
        val summaries = completed.map { summarize(it) }
        val cutoff = nowUtcMillis - THIRTY_DAYS_MILLIS
        val mostLogged = completed
            .flatMap { it.entries }
            .groupingBy { it.exerciseSnapshotName }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            .orEmpty()

        return AnalyticsOverview(
            totalCompletedWorkouts = summaries.size,
            workoutsLast30Days = summaries.count { (it.completedDateUtc ?: it.startedAtUtc) >= cutoff },
            totalVolumeKg = summaries.sumOf { it.totalVolumeKg },
            currentWeeklyStreak = weeklyStreak(summaries.map { it.completedDateUtc ?: it.startedAtUtc }, nowUtcMillis),
            mostLoggedExerciseName = mostLogged,
        )
    }

    /** 7/30-day counts, current weekly streak, longest gap — over distinct workout *days*. */
    fun consistency(
        completedDateMillis: List<Long>,
        nowUtcMillis: Long = System.currentTimeMillis(),
    ): WorkoutConsistencySnapshot {
        val days = completedDateMillis.map { toUtcDate(it) }.distinct().sorted()
        val today = toUtcDate(nowUtcMillis)

        var longestGap = 0
        for (i in 1 until days.size) {
            longestGap = maxOf(longestGap, (days[i].toEpochDay() - days[i - 1].toEpochDay()).toInt())
        }

        return WorkoutConsistencySnapshot(
            workoutsLast7Days = days.count { it >= today.minusDays(7) },
            workoutsLast30Days = days.count { it >= today.minusDays(30) },
            currentWeeklyStreak = weeklyStreak(completedDateMillis, nowUtcMillis),
            longestGapDays = longestGap,
        )
    }

    /**
     * Consecutive ISO-week streak ending at the current week (ported from MAUI `CalculateWeeklyStreak`).
     */
    fun weeklyStreak(dateMillis: List<Long>, nowUtcMillis: Long = System.currentTimeMillis()): Int {
        val buckets = dateMillis.map { isoWeekKey(toUtcDate(it)) }.toHashSet()
        if (buckets.isEmpty()) return 0

        var streak = 0
        var cursor = weekStart(toUtcDate(nowUtcMillis))
        while (isoWeekKey(cursor) in buckets) {
            streak++
            cursor = cursor.minusDays(7)
        }
        return streak
    }

    private fun describeSessionType(entries: List<WorkoutEntry>): String {
        val hasStrength = entries.any { it.entryType == ExerciseCategory.Strength }
        val hasCardio = entries.any { it.entryType == ExerciseCategory.Cardio }
        return when {
            hasStrength && hasCardio -> "Mixed"
            hasStrength -> "Strength"
            hasCardio -> "Cardio"
            else -> "Unspecified"
        }
    }

    private fun describeBodyParts(entries: List<WorkoutEntry>): String {
        val parts = entries
            .map { it.exerciseSnapshotPrimaryBodyPart }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()
        return if (parts.isEmpty()) "No body part tags" else parts.joinToString(" • ")
    }

    private fun toUtcDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    /** Monday-based week start (MAUI used `((int)DayOfWeek + 6) % 7`). */
    private fun weekStart(date: LocalDate): LocalDate =
        date.minusDays(((date.dayOfWeek.value + 6) % 7).toLong())

    private fun isoWeekKey(date: LocalDate): Pair<Int, Int> =
        date.get(IsoFields.WEEK_BASED_YEAR) to date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
}
