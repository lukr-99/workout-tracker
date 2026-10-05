package com.lukr99.workout.domain

import com.lukr99.workout.domain.run.Run
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The current week, Monday to Sunday, as Home shows it: a mark per day and the week's totals.
 * Only finished workouts count; a run counts on the day it started.
 */
data class TrainingWeek(
    val days: List<TrainingDay>,
    val workouts: Int,
    val liftedKg: Double,
    val runMeters: Double,
) {
    companion object {
        fun of(
            today: LocalDate,
            zone: ZoneId,
            workouts: List<WorkoutSessionSummary>,
            runs: List<Run>,
        ): TrainingWeek {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val week = monday..monday.plusDays(6)
            fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

            val finished = workouts
                .filter { it.status == WorkoutSessionStatus.Completed }
                .filter { dayOf(it.completedDateUtc ?: it.startedAtUtc) in week }
            val weekRuns = runs.filter { dayOf(it.startedAtUtc) in week }
            val liftDays = finished.map { dayOf(it.completedDateUtc ?: it.startedAtUtc) }.toSet()
            val runDays = weekRuns.map { dayOf(it.startedAtUtc) }.toSet()

            return TrainingWeek(
                days = (0L..6L).map { offset ->
                    val date = monday.plusDays(offset)
                    TrainingDay(date, lifted = date in liftDays, ran = date in runDays, isToday = date == today)
                },
                workouts = finished.size,
                liftedKg = finished.sumOf { it.totalVolumeKg },
                runMeters = weekRuns.sumOf { it.distanceMeters },
            )
        }
    }
}
