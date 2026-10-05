package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.SetTag

data class StrengthSetDraft(
    val id: String = "",
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val rir: Double? = null,
    val rpe: Double? = null,
    val performedAtUtc: Long? = null,
    val notes: String = "",
    val isWarmup: Boolean = false,
    val isPr: Boolean = false,
    val durationSeconds: Int? = null,
    val setType: SetType = SetType.Normal,
    val tags: Set<SetTag> = emptySet(),
)
