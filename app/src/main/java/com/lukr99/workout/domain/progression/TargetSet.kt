package com.lukr99.workout.domain.progression

import com.lukr99.workout.domain.SetType

data class TargetSet(
    val reps: Int,
    val weightKg: Double,
    val setType: SetType = SetType.Normal,
)
