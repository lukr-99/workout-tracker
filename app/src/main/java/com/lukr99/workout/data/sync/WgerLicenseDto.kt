package com.lukr99.workout.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WgerLicenseDto(
    @SerialName("short_name")
    val shortName: String = "",
    val url: String = "",
)
