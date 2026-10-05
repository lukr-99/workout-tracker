package com.lukr99.workout.domain.stats

data class StatsRow(
    val dimensions: Map<String, String>,
    val metrics: Map<String, MetricValue>,
    val sampleSize: Int,
)
