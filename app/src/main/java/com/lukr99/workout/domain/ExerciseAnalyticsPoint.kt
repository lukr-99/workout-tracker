package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseAnalyticsPoint(
    val exerciseId: String = "",
    @Serializable(with = InstantMillisSerializer::class)
    val dateUtc: Long = 0,
    val bestWeightKg: Double = 0.0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val sessionName: String = "",
)
