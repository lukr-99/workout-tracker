package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

/** Origin of a persisted session. Appended-only; ordinal values are part of the v1.2 wire schema. */
@Serializable(with = WorkoutSessionSourceSerializer::class)
enum class WorkoutSessionSource { Local, HealthConnect }
