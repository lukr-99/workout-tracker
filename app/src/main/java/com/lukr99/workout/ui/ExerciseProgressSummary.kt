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
)
