package com.lukr99.workout.domain.recovery

data class MuscleRecovery(
    val bodyPart: String,
    /** The known muscle this row is, or null for a custom body part or Cardio. */
    val group: MuscleGroup?,
    val readiness: Double,
    val state: RecoveryState,
    /** Remaining fatigue in effort-weighted hard sets. */
    val fatigueLoad: Double,
    val lastTrainedAtUtc: Long?,
    val readyAtUtc: Long?,
    val weeklyVolumeKg: Double,
    /** Hard sets in the last seven days. Fractional because a secondary muscle counts half a set. */
    val weeklySetCount: Double,
    /** Null for parts that have no weekly target, like Cardio or a custom body part. */
    val weeklyStatus: WeeklyVolumeStatus?,
)
