package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** One of Claude's proposals waiting in the inbox; [payload] is a [ProposedExercise] or a [ProposedTemplate]. */
@Serializable
data class Proposal(
    val id: String,
    /** "exercise" or "template". */
    val kind: String,
    val status: String,
    val title: String,
    val reason: String,
    val payload: JsonObject,
    val createdAt: String,
) {
    val isTemplate: Boolean get() = kind == "template"

    fun exercise(): ProposedExercise = PAYLOAD_JSON.decodeFromJsonElement(ProposedExercise.serializer(), payload)

    fun template(): ProposedTemplate = PAYLOAD_JSON.decodeFromJsonElement(ProposedTemplate.serializer(), payload)

    private companion object {
        val PAYLOAD_JSON = Json { ignoreUnknownKeys = true }
    }
}
