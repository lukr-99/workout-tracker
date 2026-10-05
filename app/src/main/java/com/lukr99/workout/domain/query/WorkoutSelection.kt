package com.lukr99.workout.domain.query

import com.lukr99.workout.domain.WorkoutSession

data class WorkoutSelection(
    val points: List<WorkoutDataPoint>,
    val matchedSessionIds: Set<String>,
    val matchedEntryIds: Set<String>,
) {
    val sessions: List<WorkoutSession>
        get() = points.distinctBy { it.session.id }.map { it.session }
}
