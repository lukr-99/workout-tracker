package com.lukr99.workout.data.transfer

data class ImportCommitResult(
    val format: DataFormat,
    val insertedExercises: Int,
    val changedExercises: Int,
    val insertedTemplates: Int,
    val changedTemplates: Int,
    val insertedSessions: Int,
    val changedSessions: Int,
    val skippedSessions: Int,
    val insertedRuns: Int = 0,
    val insertedRoutes: Int = 0,
    val restoredPhotos: Int = 0,
    val restoredSettings: Boolean = false,
    val mode: RestoreMode = RestoreMode.Merge,
    val issues: List<TransferIssue>,
)
