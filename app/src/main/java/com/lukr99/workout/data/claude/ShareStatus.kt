package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** When the server's copy was shared and when it is deleted, as ISO-8601 UTC. */
@Serializable
data class ShareStatus(val sharedAt: String, val keepUntil: String)
