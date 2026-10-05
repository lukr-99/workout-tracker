package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutConsistencySnapshot(
    val workoutsLast7Days: Int = 0,
    val workoutsLast30Days: Int = 0,
    val currentWeeklyStreak: Int = 0,
    val longestGapDays: Int = 0,
)
