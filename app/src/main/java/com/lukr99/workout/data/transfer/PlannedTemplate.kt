package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.WorkoutTemplate

data class PlannedTemplate(
    val value: WorkoutTemplate,
    val action: PlannedAction,
    val targetId: String? = null,
    val reason: String = "",
)
