package com.lukr99.workout.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WgerNamedDto(
    val id: Int? = null,
    val name: String = "",
    @SerialName("name_en")
    val englishName: String? = null,
) {
    internal fun displayName(): String = englishName?.trim()
        ?.takeIf(String::isNotBlank)
        ?: name.trim()
}
