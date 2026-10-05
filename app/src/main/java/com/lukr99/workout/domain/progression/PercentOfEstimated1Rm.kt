package com.lukr99.workout.domain.progression

data class PercentOfEstimated1Rm(
    val percentage: Double = 0.75,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    override val roundingIncrementKg: Double = 0.5,
) : ProgressionScheme {
    override val key: String = "percent_e1rm"
}
