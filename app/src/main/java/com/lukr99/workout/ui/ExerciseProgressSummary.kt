package com.lukr99.workout.ui

import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.LocationScope

data class ExerciseProgressSummary(
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val category: ExerciseCategory,
    val bestE1rmKg: Double,
    val latestE1rmKg: Double,
    val deltaKg: Double,
    val points: List<ProgressPoint>,
    /** Which workouts the numbers come from; per location for an exercise tracked that way. */
    val scope: LocationScope = LocationScope.Shared,
    /** The same workouts split by place, for the "All gyms" chart. Every workout counts here. */
    val series: List<LocationSeries> = emptyList(),
) {
    /**
     * Whether the detail screen offers the gym switch. A per-gym exercise always can, to look past
     * the current gym. A shared one only when its workouts span more than one place.
     */
    val canSplitByGym: Boolean get() = if (scope.perLocation) series.isNotEmpty() else series.size >= 2

    /** The switch's first option: the line the chart opens on. */
    val defaultViewLabel: String get() = if (scope.perLocation) "This gym" else "All gyms"

    /** The switch's second option: one line per place. */
    val splitViewLabel: String get() = if (scope.perLocation) "All gyms" else "By gym"
}
