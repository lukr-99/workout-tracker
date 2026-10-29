package com.lukr99.workout.domain.recovery

data class RecoverySnapshot(
    val calculatedAtUtc: Long,
    val muscles: List<MuscleRecovery>,
    val averageReadiness: Double,
    val config: RecoveryConfig = RecoveryConfig(),
) {
    /** The row for a body part name or any of its aliases, like Quadriceps for Quads. */
    fun forBodyPart(bodyPart: String): MuscleRecovery? {
        val group = MuscleGroups.groupOf(bodyPart)
        return muscles.firstOrNull {
            (group != null && it.group == group) || it.bodyPart.equals(bodyPart.trim(), ignoreCase = true)
        }
    }

    fun forGroup(group: MuscleGroup): MuscleRecovery? = muscles.firstOrNull { it.group == group }

    /** Every known muscle in body order, with a fresh row for one not trained in the lookback. */
    fun allGroups(): List<MuscleRecovery> = MuscleGroup.entries.map { group ->
        forGroup(group) ?: MuscleRecovery(
            bodyPart = group.label,
            group = group,
            readiness = 100.0,
            state = RecoveryState.Ready,
            fatigueLoad = 0.0,
            lastTrainedAtUtc = null,
            readyAtUtc = null,
            weeklyVolumeKg = 0.0,
            weeklySetCount = 0.0,
            weeklyStatus = WeeklyVolumeStatus.None,
        )
    }
}
