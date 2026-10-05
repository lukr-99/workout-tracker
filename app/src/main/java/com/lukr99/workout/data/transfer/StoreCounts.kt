package com.lukr99.workout.data.transfer

/** How much the store holds: what a replace-restore or "Delete all data" removes. */
data class StoreCounts(
    val exercises: Int = 0,
    val templates: Int = 0,
    val workouts: Int = 0,
    val runs: Int = 0,
    val routes: Int = 0,
)
