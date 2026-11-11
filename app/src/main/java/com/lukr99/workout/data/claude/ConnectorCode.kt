package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** A single-use code for Claude's sign-in page, with the connector URL to add in Claude. */
@Serializable
data class ConnectorCode(val code: String, val mcpUrl: String, val expiresAt: String)
