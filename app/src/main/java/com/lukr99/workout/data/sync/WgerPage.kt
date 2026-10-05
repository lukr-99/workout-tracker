package com.lukr99.workout.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class WgerPage(
    val count: Int = 0,
    val next: String? = null,
    val results: List<WgerExerciseDto> = emptyList(),
)
