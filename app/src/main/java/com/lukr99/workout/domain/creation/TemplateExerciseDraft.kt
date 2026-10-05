package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.ExerciseCategory

data class TemplateExerciseDraft(
    val id: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val bodyPart: String = "",
    val notes: String = "",
)
