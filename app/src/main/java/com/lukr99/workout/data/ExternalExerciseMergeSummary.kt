package com.lukr99.workout.data

/** Structured outcome for a history-safe external catalog merge. */
data class ExternalExerciseMergeSummary(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
) {
    val changed: Int get() = added + updated

    operator fun plus(other: ExternalExerciseMergeSummary) = ExternalExerciseMergeSummary(
        added = added + other.added,
        updated = updated + other.updated,
        skipped = skipped + other.skipped,
    )
}
