package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSessionSummary(
    val id: String = "",
    val name: String = "",
    @Serializable(with = InstantMillisSerializer::class)
    val startedAtUtc: Long = 0,
    @Serializable(with = InstantMillisSerializer::class)
    val completedDateUtc: Long? = null,
    val status: WorkoutSessionStatus = WorkoutSessionStatus.Completed,
    val exerciseCount: Int = 0,
    val strengthSetCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val cardioMinutes: Int = 0,
    val sessionTypeLabel: String = "",
    val bodyPartsSummary: String = "",
)
