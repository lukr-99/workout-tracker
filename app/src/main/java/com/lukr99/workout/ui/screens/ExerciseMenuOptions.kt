package com.lukr99.workout.ui.screens

/** What the exercise menu may offer for one exercise in the live workout. */
internal data class ExerciseMenuOptions(
    val canSupersetWithPrevious: Boolean,
    val groupedWithPrevious: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val hasGuide: Boolean,
)
