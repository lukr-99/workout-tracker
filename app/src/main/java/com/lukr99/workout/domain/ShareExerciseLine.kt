package com.lukr99.workout.domain

/**
 * One exercise as it appears on a shared workout: its name, a short [detail] (the best working set
 * for a lift, time and distance for cardio) and whether it set a personal record.
 */
data class ShareExerciseLine(
    val name: String,
    val detail: String,
    val hasPr: Boolean,
)
