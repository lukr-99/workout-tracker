package com.lukr99.workout.domain.records

data class SessionVolumeRecord(
    val volumeKg: Double,
    val setCount: Int,
    val source: RecordSource,
)
