package com.lukr99.workout.ui.screens

/** What the exercise menu may offer for one exercise in the live workout. */
internal data class ExerciseMenuOptions(
    /** Already in a superset: the menu offers to leave it. */
    val inSuperset: Boolean,
    /** The exercise after this one, to pair with; the menu prefers it over [previousName]. */
    val nextName: String?,
    /** The exercise before this one, for the last exercise in the workout. */
    val previousName: String?,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val hasGuide: Boolean,
)
