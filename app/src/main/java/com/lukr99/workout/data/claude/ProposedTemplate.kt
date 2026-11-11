package com.lukr99.workout.data.claude

import kotlinx.serialization.Serializable

/** A template Claude proposed; with [replacesTemplateId] it is a new version of that template. */
@Serializable
data class ProposedTemplate(
    val templateId: String,
    val replacesTemplateId: String? = null,
    val name: String,
    val notes: String = "",
    val exercises: List<ProposedTemplateExercise>,
)
