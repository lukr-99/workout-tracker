package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** Claude asking for the training data; the phone answers it within ten minutes or it lapses. */
@Serializable
data class ShareRequest(val id: String, val createdAt: String, val expiresAt: String)
