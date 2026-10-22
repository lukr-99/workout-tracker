package com.lukr99.workout.ui

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession

data class ProgressPoint(
    val dateMillis: Long,
    val e1rmKg: Double,
    val volumeKg: Double,
    val reps: Int,
    val sessionName: String,
) {
    companion object {
        /** One workout's contribution to an exercise's chart, from that workout's [sets]. */
        fun of(session: WorkoutSession, sets: List<StrengthSet>) = ProgressPoint(
            dateMillis = session.completedDateUtc ?: session.startedAtUtc,
            e1rmKg = Estimates.bestEpley(sets),
            volumeKg = Estimates.volume(sets),
            reps = Estimates.totalReps(sets),
            sessionName = session.name,
        )
    }
}
