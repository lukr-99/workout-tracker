package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** A device the server knows: this phone ("android") or a Claude connector ("connector"). */
@Serializable
data class ClaudeDevice(
    val id: String,
    val name: String,
    val platform: String,
    val scopes: List<String> = emptyList(),
    val revokedAt: String? = null,
) {
    val isActiveConnector: Boolean get() = platform == "connector" && revokedAt == null
}
