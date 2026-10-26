package com.lukr99.workout.domain

import com.lukr99.workout.domain.run.Run
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

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
            zone: TimeZone,
            workouts: List<WorkoutSessionSummary>,
            runs: List<Run>,
        ): TrainingWeek {
            val monday = today.mondayOfWeek()
            val week = monday..monday.plusDays(6)
            fun dayOf(millis: Long) = millis.dateIn(zone)

            val finished = workouts
                .filter { it.status == WorkoutSessionStatus.Completed }
                .filter { dayOf(it.completedDateUtc ?: it.startedAtUtc) in week }
            val weekRuns = runs.filter { dayOf(it.startedAtUtc) in week }
            val activities = finished.map {
                DayActivity(it.id, isRun = false, name = it.name, atUtc = it.completedDateUtc ?: it.startedAtUtc)
            } + weekRuns.map {
                DayActivity(it.id, isRun = true, name = it.title, atUtc = it.startedAtUtc)
            }
            val byDay = activities.sortedBy(DayActivity::atUtc).groupBy { dayOf(it.atUtc) }

            return TrainingWeek(
                days = (0..6).map { offset ->
                    val date = monday.plusDays(offset)
                    TrainingDay(date, isToday = date == today, activities = byDay[date].orEmpty())
                },
                workouts = finished.size,
                liftedKg = finished.sumOf { it.totalVolumeKg },
                runMeters = weekRuns.sumOf { it.distanceMeters },
            )
        }
    }
}
