package com.lukr99.workout.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WgerImageDto(
    val image: String? = null,
    @SerialName("is_main")
    val isMain: Boolean = false,
)
