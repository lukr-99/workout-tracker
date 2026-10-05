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
    // Export 1.9: the plan for this exercise. Null means "not set"; a template without a plan
    // still starts with one empty set, as before.
    val targetSets: Int? = null,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val restSeconds: Int? = null,
    /** Exercises that share a number are done as one superset. */
    val supersetGroup: Int? = null,
)
