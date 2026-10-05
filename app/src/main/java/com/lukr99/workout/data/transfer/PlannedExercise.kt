package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.Exercise

data class PlannedExercise(
    val value: Exercise,
    val action: PlannedAction,
    val targetId: String? = null,
    val reason: String = "",
)
