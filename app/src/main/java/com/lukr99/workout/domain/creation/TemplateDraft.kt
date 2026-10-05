package com.lukr99.workout.domain.creation

data class TemplateDraft(
    val id: String = "",
    val name: String = "",
    val notes: String = "",
    val exercises: List<TemplateExerciseDraft> = emptyList(),
)
