package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable(with = WorkoutSessionStatusSerializer::class)
enum class WorkoutSessionStatus { Active, Completed, Discarded } // Active=0, Completed=1, Discarded=2
