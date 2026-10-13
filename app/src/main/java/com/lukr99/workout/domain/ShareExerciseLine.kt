package com.lukr99.workout.domain

/**
 * One exercise as it appears on a shared workout: its name, a short [detail] (the best working set
 * for a lift, time and distance for cardio), whether it set a personal record, and its
 * [workingSets] without units (`100 x 5`) for a card that has room to show them.
 */
data class ShareExerciseLine(
    val name: String,
    val detail: String,
    val hasPr: Boolean,
    val workingSets: List<String> = emptyList(),
)
