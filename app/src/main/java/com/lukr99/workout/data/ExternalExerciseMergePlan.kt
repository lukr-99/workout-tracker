package com.lukr99.workout.data

import com.lukr99.workout.domain.Exercise

/** What an external catalog merge will write: the rows to upsert, in order, and the counts. */
data class ExternalExerciseMergePlan(
    val upserts: List<Exercise>,
    val summary: ExternalExerciseMergeSummary,
)
