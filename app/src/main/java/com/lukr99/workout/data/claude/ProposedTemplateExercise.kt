package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** One exercise of a proposed template with its plan; [newExercise] is set when Claude proposed it too. */
@Serializable
data class ProposedTemplateExercise(
    val exerciseId: String,
    val name: String,
    val category: String,
    val bodyPart: String,
    val notes: String = "",
    val targetSets: Int? = null,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val restSeconds: Int? = null,
    val supersetGroup: Int? = null,
    val newExercise: ProposedExercise? = null,
)
