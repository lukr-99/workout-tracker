package com.lukr99.workout.domain.records

data class SetRecord(
    /** The metric value represented by this record (kg, e1RM kg, or set-volume kg). */
    val value: Double,
    val weightKg: Double,
    val reps: Int,
    val setVolumeKg: Double,
    val estimated1RmKg: Double,
    val source: RecordSource,
)
