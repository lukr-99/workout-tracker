package com.lukr99.workout.domain.progression

data class DoubleProgression(
    val repRange: IntRange = 8..12,
    val weightIncrementKg: Double = 2.5,
    val targetSets: Int? = null,
    override val roundingIncrementKg: Double = 0.5,
) : ProgressionScheme {
    override val key: String = "double_progression"
}
