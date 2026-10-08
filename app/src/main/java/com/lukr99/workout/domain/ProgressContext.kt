package com.lukr99.workout.domain

/** The context lines under the Progress tiles: recent windows next to all-time totals. */
data class ProgressContext(
    val workoutsThisMonth: Int = 0,
    val volumeLast4WeeksKg: Double = 0.0,
    /** Volume in the four weeks before the last four, to compare against; 0 when there were none. */
    val volumePrevious4WeeksKg: Double = 0.0,
    val prSetsLast30Days: Int = 0,
    val volumeThisWeekKg: Double = 0.0,
    val bestStreakWeeks: Int = 0,
) {
    /** Change from the previous four weeks in whole percent, or null with nothing to compare. */
    val volumeChangePercent: Int?
        get() = if (volumePrevious4WeeksKg <= 0.0) null else Math.round((volumeLast4WeeksKg / volumePrevious4WeeksKg - 1) * 100).toInt()

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000

        /**
         * From finished workouts at [now] in [zone]. A workout counts at its finish time. The week
         * starts on Monday, like the Home week strip.
         */
        fun of(sessions: List<WorkoutSession>, now: Long, zone: java.time.ZoneId): ProgressContext {
            val done = sessions.filter { it.status == WorkoutSessionStatus.Completed }
            fun at(s: WorkoutSession) = s.completedDateUtc ?: s.startedAtUtc
            fun volume(list: List<WorkoutSession>) = list.sumOf { s -> s.entries.sumOf { Estimates.volume(it.strengthSets) } }
            val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            return ProgressContext(
                workoutsThisMonth = done.count { at(it) >= monthStart },
                volumeLast4WeeksKg = volume(done.filter { at(it) > now - 28 * DAY_MS }),
                volumePrevious4WeeksKg = volume(done.filter { at(it) in (now - 56 * DAY_MS + 1)..(now - 28 * DAY_MS) }),
                prSetsLast30Days = done.filter { at(it) > now - 30 * DAY_MS }.sumOf { s -> s.entries.sumOf { e -> e.strengthSets.count { it.isPr } } },
                volumeThisWeekKg = volume(done.filter { at(it) >= weekStart }),
                bestStreakWeeks = Analytics.bestWeeklyStreak(done.map(::at)),
            )
        }
    }
}
