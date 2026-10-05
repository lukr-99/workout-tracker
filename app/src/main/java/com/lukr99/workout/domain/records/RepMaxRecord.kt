package com.lukr99.workout.domain.records

data class RepMaxRecord(
    val reps: Int,
    val weightKg: Double,
    val estimated1RmKg: Double,
    val source: RecordSource,
)
