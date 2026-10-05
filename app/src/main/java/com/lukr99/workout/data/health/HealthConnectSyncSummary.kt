package com.lukr99.workout.data.health

data class HealthConnectSyncSummary(
    val imported: Int = 0,
    val exported: Int = 0,
    val skipped: Int = 0,
    val unsupported: Int = 0,
)
