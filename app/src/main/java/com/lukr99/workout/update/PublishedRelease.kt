package com.lukr99.workout.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The latest published GitHub Release: its tag, notes and attached files. */
@Serializable
data class PublishedRelease(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<ReleaseAsset> = emptyList(),
) {
    /** The tag without its leading `v`, like "2.6.0". */
    val version: String get() = tag.trim().removePrefix("v")
}
