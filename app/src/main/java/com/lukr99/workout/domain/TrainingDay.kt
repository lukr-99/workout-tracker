package com.lukr99.workout.domain

import java.time.LocalDate

/** One day of the training week on Home: whether it had a finished workout, a run, or both. */
data class TrainingDay(
    val date: LocalDate,
    val lifted: Boolean,
    val ran: Boolean,
    val isToday: Boolean,
) {
    val trained: Boolean get() = lifted || ran
}
