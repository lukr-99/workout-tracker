package com.lukr99.workout.domain

import kotlinx.datetime.LocalDate

/** One day of the training week on Home: its finished workouts and runs, oldest first. */
data class TrainingDay(
    val date: LocalDate,
    val isToday: Boolean,
    val activities: List<DayActivity> = emptyList(),
) {
    val lifted: Boolean get() = activities.any { !it.isRun }
    val ran: Boolean get() = activities.any { it.isRun }
    val trained: Boolean get() = activities.isNotEmpty()
}
