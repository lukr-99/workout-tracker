package com.lukr99.workout.data.transfer

data class ImportSummary(
    val sourceRows: Int = 0,
    val parsedSessions: Int = 0,
    val insertedSessions: Int = 0,
    val changedSessions: Int = 0,
    val skippedSessions: Int = 0,
    val insertedExercises: Int = 0,
    val matchedExercises: Int = 0,
    val templates: Int = 0,
    val setCount: Int = 0,
    val insertedRuns: Int = 0,
    val insertedRoutes: Int = 0,
    val photos: Int = 0,
    val dateFromUtc: Long? = null,
    val dateToUtc: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
)
