package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutQuery
import kotlinx.datetime.TimeZone

data class StatsRequest(
    val query: WorkoutQuery = WorkoutQuery(),
    val metrics: List<String> = listOf(
        MetricKeys.Workouts,
        MetricKeys.Sets,
        MetricKeys.Reps,
        MetricKeys.VolumeKg,
    ),
    val dimensions: List<String> = emptyList(),
    val timeZoneId: String = TimeZone.currentSystemDefault().id,
    val sort: List<StatsSort> = emptyList(),
)
