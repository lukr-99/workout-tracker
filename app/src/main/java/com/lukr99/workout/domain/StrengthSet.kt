package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class StrengthSet(
    val id: String = newId(),
    val workoutEntryId: String = "",
    val setNumber: Int = 0,
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val rir: Double? = null,
    val rpe: Double? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val performedAtUtc: Long? = null,
    val notes: String = "",
    // Rework-additive (03-data-model.md).
    val isWarmup: Boolean = false,
    val isPr: Boolean = false,
    val durationSeconds: Int? = null,
    val setType: SetType = SetType.Normal,
    /** Combinable tags. Empty means a legacy [setType] value has not been upgraded yet. */
    val tags: Set<SetTag> = emptySet(),
)
