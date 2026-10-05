package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplateExercise(
    val id: String = newId(),
    val exerciseId: String = "",
    val exerciseName: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val bodyPart: String = "",
    val sortOrder: Int = 0,
    val notes: String = "",
)
