package com.lukr99.workout.domain

/** One way a finished workout differs from the template it started from. */
sealed interface TemplateChange {
    val exerciseName: String

    data class Added(override val exerciseName: String) : TemplateChange
    data class Skipped(override val exerciseName: String) : TemplateChange
    data class SetCount(override val exerciseName: String, val done: Int, val planned: Int) : TemplateChange
}
