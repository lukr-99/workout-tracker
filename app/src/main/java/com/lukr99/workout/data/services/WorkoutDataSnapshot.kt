package com.lukr99.workout.data.services

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate

data class WorkoutDataSnapshot(
    val exercises: List<Exercise>,
    val templates: List<WorkoutTemplate>,
    val sessions: List<WorkoutSession>,
)
