package com.lukr99.workout.data.claude

/** The Claude server (the Cloudflare Worker in cloud/) this phone is set up with, and its token there. */
data class ClaudeLink(val serverUrl: String, val token: String)
