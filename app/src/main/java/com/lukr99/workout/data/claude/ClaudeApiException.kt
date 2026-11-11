package com.lukr99.workout.data.claude

/** A refusal from the Claude server, `{"error": {"code", "message"}}`, or a network failure (status 0). */
class ClaudeApiException(val status: Int, val code: String, message: String) : Exception(message)
