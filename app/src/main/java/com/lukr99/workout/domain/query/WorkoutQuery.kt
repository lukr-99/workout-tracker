package com.lukr99.workout.domain.query

/**
 * A composable, storage-agnostic query tree. The same query can drive stats, CSV exports,
 * dashboards, future sync selection, or an in-memory preview without adding one-off repository
 * methods for every filter combination.
 */
data class WorkoutQuery(
    val filter: WorkoutFilter = WorkoutFilter.All,
    val includeEmptySessions: Boolean = false,
    val includeEmptyEntries: Boolean = false,
)

infix fun WorkoutFilter.and(other: WorkoutFilter): WorkoutFilter = when {
    this == WorkoutFilter.All -> other
    other == WorkoutFilter.All -> this
    this == WorkoutFilter.None || other == WorkoutFilter.None -> WorkoutFilter.None
    this is WorkoutFilter.And && other is WorkoutFilter.And -> WorkoutFilter.And(filters + other.filters)
    this is WorkoutFilter.And -> WorkoutFilter.And(filters + other)
    other is WorkoutFilter.And -> WorkoutFilter.And(listOf(this) + other.filters)
    else -> WorkoutFilter.And(listOf(this, other))
}

infix fun WorkoutFilter.or(other: WorkoutFilter): WorkoutFilter = when {
    this == WorkoutFilter.None -> other
    other == WorkoutFilter.None -> this
    this == WorkoutFilter.All || other == WorkoutFilter.All -> WorkoutFilter.All
    this is WorkoutFilter.Or && other is WorkoutFilter.Or -> WorkoutFilter.Or(filters + other.filters)
    this is WorkoutFilter.Or -> WorkoutFilter.Or(filters + other)
    other is WorkoutFilter.Or -> WorkoutFilter.Or(listOf(this) + other.filters)
    else -> WorkoutFilter.Or(listOf(this, other))
}

operator fun WorkoutFilter.not(): WorkoutFilter = WorkoutFilter.Not(this)

fun WorkoutCriterion.asFilter(): WorkoutFilter = WorkoutFilter.Criterion(this)
