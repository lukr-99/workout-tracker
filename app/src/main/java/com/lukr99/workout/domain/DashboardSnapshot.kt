package com.lukr99.workout.domain

data class DashboardSnapshot(
    val activeWorkout: WorkoutSession? = null,
    val analytics: AnalyticsOverview = AnalyticsOverview(),
    val consistency: WorkoutConsistencySnapshot = WorkoutConsistencySnapshot(),
    val recentWorkouts: List<WorkoutSessionSummary> = emptyList(),
)
