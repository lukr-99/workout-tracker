package com.lukr99.workout.data.images

import kotlinx.serialization.Serializable

@Serializable
data class FreeExerciseImageEntry(
    val images: List<String> = emptyList(),
    val muscle: String? = null,
    val equipment: String? = null,
)
