package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable(with = ExerciseSourceSerializer::class)
enum class ExerciseSource { Seeded, Synced, Custom } // Seeded=0, Synced=1, Custom=2
