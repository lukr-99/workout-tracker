package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** An exercise Claude proposed, with the id it gets here when the owner accepts it. */
@Serializable
data class ProposedExercise(
    val exerciseId: String,
    val name: String,
    /** "strength" or "cardio". */
    val category: String,
    val primaryBodyPart: String,
    val secondaryBodyParts: List<String> = emptyList(),
    val equipment: String = "",
    val notes: String = "",
    val instructions: String = "",
    val defaultRestSeconds: Int? = null,
)
