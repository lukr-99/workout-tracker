package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplate(
    val id: String = newId(),
    val name: String = "",
    val notes: String = "",
    val exercises: List<WorkoutTemplateExercise> = emptyList(),
)
