package com.lukr99.workout.ui.screens

/**
 * One row of the template being edited. [rowId] is the template exercise's own id, so the same
 * exercise can appear twice (a heavy and a back-off block) and a save keeps ids and notes.
 */
internal data class TemplateRow(
    val rowId: String,
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val notes: String,
)
