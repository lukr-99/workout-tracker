package com.lukr99.workout.domain.query

sealed interface WorkoutFilter {
    data object All : WorkoutFilter
    data object None : WorkoutFilter
    data class Criterion(val criterion: WorkoutCriterion) : WorkoutFilter
    data class And(val filters: List<WorkoutFilter>) : WorkoutFilter
    data class Or(val filters: List<WorkoutFilter>) : WorkoutFilter
    data class Not(val filter: WorkoutFilter) : WorkoutFilter
}
