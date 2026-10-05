package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.Exercise

data class CreationPolicy(
    val requireCatalogExercise: Boolean = false,
    val requireSessionEntries: Boolean = false,
    val requireTemplateExercises: Boolean = false,
    val fallbackExerciseName: String = "Custom Exercise",
    val fallbackBodyPart: String = "Full Body",
    val fallbackTemplateName: String = "Untitled Template",
    val fallbackSessionName: String = "Quick Workout",
    val restSecondsRange: IntRange = 0..3_600,
)
