package com.lukr99.workout.domain.progression

data class LinearProgression(
    val targetSets: Int = 3,
    val targetReps: Int = 5,
    val weightIncrementKg: Double = 2.5,
    override val roundingIncrementKg: Double = 0.5,
) : ProgressionScheme {
    override val key: String = "linear"
}
