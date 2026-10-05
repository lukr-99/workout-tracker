package com.lukr99.workout.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One file attached to a GitHub Release, as the Releases API describes it. */
@Serializable
data class ReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)
