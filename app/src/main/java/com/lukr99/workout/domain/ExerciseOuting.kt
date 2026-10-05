package com.lukr99.workout.domain

/** One earlier time an exercise was logged: when, its working sets, and the best estimated 1RM. */
data class ExerciseOuting(
    val atUtc: Long,
    val sets: List<StrengthSet>,
    val bestE1rmKg: Double,
)
