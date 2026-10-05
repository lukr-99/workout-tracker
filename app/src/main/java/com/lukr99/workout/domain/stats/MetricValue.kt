package com.lukr99.workout.domain.stats

data class MetricValue(
    val value: Double,
    val unit: MetricUnit = MetricUnit.Count,
)
