package com.lukr99.workout.ui

import com.lukr99.workout.domain.ExerciseCategory

data class ExerciseProgressSummary(
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val category: ExerciseCategory,
    val bestE1rmKg: Double,
    val latestE1rmKg: Double,
    val deltaKg: Double,
    val points: List<ProgressPoint>,
)
