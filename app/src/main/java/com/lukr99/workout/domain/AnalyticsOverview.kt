package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsOverview(
    val totalCompletedWorkouts: Int = 0,
    val workoutsLast30Days: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val currentWeeklyStreak: Int = 0,
    val mostLoggedExerciseName: String = "",
)
