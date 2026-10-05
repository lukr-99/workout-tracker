package com.lukr99.workout.domain.stats

data class StatsReport(
    val request: StatsRequest,
    val rows: List<StatsRow>,
    val matchedSessions: Int,
    val matchedEntries: Int,
    val matchedPoints: Int,
    val availableMetricKeys: Set<String>,
    val availableDimensionKeys: Set<String>,
)
