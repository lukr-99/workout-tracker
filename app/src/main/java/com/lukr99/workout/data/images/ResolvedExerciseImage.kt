package com.lukr99.workout.data.images

data class ResolvedExerciseImage(
    val model: Any,
    val source: ExerciseImageSource,
    val attribution: String? = null,
)
