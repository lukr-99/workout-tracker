package com.lukr99.workout.ui

data class ProgressPoint(
    val dateMillis: Long,
    val e1rmKg: Double,
    val volumeKg: Double,
    val reps: Int,
    val sessionName: String,
)
