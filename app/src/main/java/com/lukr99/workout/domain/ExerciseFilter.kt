package com.lukr99.workout.domain

/** Catalog query filter (ported from MAUI `ExerciseFilter`). Applied in the repository. */
data class ExerciseFilter(
    val searchText: String = "",
    val bodyPart: String = "",
    val category: ExerciseCategory? = null,
    val equipment: String = "",
    val includeArchived: Boolean = false,
)
