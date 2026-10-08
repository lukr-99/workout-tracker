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
) {
    /**
     * The rep range moved by [by] as a whole, so 6–8 becomes 7–9. With no reps planned, + starts at
     * [START_REPS] and - leaves it empty. Reps never go below 1.
     */
    fun withRepsShifted(by: Int): TemplateRow {
        val low = repsMin ?: repsMax
        val high = repsMax ?: repsMin
        if (low == null || high == null) return if (by > 0) copy(repsMin = START_REPS, repsMax = START_REPS) else this
        val step = by.coerceAtLeast(1 - low)
        return copy(repsMin = low + step, repsMax = high + step)
    }

    companion object {
        const val START_REPS = 8
    }
}
