package com.lukr99.workout.ui.screens

/**
 * One row of the template being edited. [rowId] is the template exercise's own id, so the same
 * exercise can appear twice (a heavy and a back-off block) and a save keeps ids, notes and plan.
 */
internal data class TemplateRow(
    val rowId: String,
    val exerciseId: String,
    val name: String,
    val bodyPart: String,
    val notes: String,
    val targetSets: Int? = null,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val restSeconds: Int? = null,
    val supersetGroup: Int? = null,
)
