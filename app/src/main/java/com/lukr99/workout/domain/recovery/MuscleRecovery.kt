package com.lukr99.workout.domain.recovery

data class MuscleRecovery(
    val bodyPart: String,
    val readiness: Double,
    val fatigueLoad: Double,
    val lastTrainedAtUtc: Long?,
    val readyAtUtc: Long?,
    val weeklyVolumeKg: Double,
    /** Fractional when secondary-muscle contribution is below 1. */
    val weeklySetCount: Double,
    val weeklyLoadUnits: Double,
)
