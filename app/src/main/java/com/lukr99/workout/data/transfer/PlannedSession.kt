package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.WorkoutSession

data class PlannedSession(
    val value: WorkoutSession,
    val action: PlannedAction,
    val targetId: String? = null,
    val reason: String = "",
)
