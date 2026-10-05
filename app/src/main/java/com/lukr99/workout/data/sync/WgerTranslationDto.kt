package com.lukr99.workout.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WgerTranslationDto(
    val language: Int? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("description_source")
    val descriptionSource: String? = null,
)
