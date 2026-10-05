package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate

data class ImportContext(
    val exercises: List<Exercise>,
    val templates: List<WorkoutTemplate>,
    val sessions: List<WorkoutSession>,
    /** Ids of runs/routes already in the store, so a restore only inserts the ones that are missing. */
    val existingRunIds: Set<String> = emptySet(),
    val existingRouteIds: Set<String> = emptySet(),
)
