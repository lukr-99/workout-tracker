package com.lukr99.workout.ui

data class ProgressOverview(
    val workouts: Int = 0,
    val volumeKg: Double = 0.0,
    val sets: Int = 0,
    val prSets: Int = 0,
    val streakWeeks: Int = 0,
)
