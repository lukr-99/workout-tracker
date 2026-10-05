package com.lukr99.workout.ui.run

import com.lukr99.workout.domain.run.RunStats

/** Derived running analytics for the Progress tab (empty until the first run is recorded). */
data class RunningStats(
    val totals: RunStats.Totals = RunStats.Totals(0, 0.0, 0, 0.0, 0.0),
    val weekly: List<RunStats.PeriodBucket> = emptyList(),
    val paceTrend: List<Pair<Long, Double>> = emptyList(),
    val streakWeeks: Int = 0,
    val records: RunStats.PersonalRecords = RunStats.PersonalRecords(),
) {
    val hasRuns: Boolean get() = totals.runCount > 0
}
