package com.lukr99.workout.domain.recovery

data class RecoverySnapshot(
    val calculatedAtUtc: Long,
    val muscles: List<MuscleRecovery>,
    val averageReadiness: Double,
) {
    fun forBodyPart(bodyPart: String): MuscleRecovery? =
        muscles.firstOrNull { it.bodyPart.equals(bodyPart, ignoreCase = true) }
}
