package com.lukr99.workout.domain.progression

sealed interface ProgressionScheme {
    val key: String
    val roundingIncrementKg: Double
}
