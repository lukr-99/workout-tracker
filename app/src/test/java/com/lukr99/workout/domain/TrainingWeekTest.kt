package com.lukr99.workout.domain

import com.lukr99.workout.domain.run.Run
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingWeekTest {

    private val zone = ZoneOffset.UTC
    private val wednesday = LocalDate.of(2026, 10, 7)

    private fun at(date: LocalDate, hour: Int = 18) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    @Test
    fun theWeekRunsMondayToSundayAndMarksToday() {
        val week = TrainingWeek.of(wednesday, zone, emptyList(), emptyList())

        assertEquals(LocalDate.of(2026, 10, 5), week.days.first().date)
        assertEquals(LocalDate.of(2026, 10, 11), week.days.last().date)
        assertEquals(listOf(false, false, true, false, false, false, false), week.days.map { it.isToday })
    }

    @Test
    fun finishedWorkoutsAndRunsMarkTheirDaysAndAddUp() {
        val monday = LocalDate.of(2026, 10, 5)
        val workouts = listOf(
            WorkoutSessionSummary(id = "a", startedAtUtc = at(monday), completedDateUtc = at(monday), totalVolumeKg = 12_000.0),
            WorkoutSessionSummary(id = "b", startedAtUtc = at(wednesday), totalVolumeKg = 9_000.0),
            WorkoutSessionSummary(id = "live", startedAtUtc = at(wednesday), status = WorkoutSessionStatus.Active, totalVolumeKg = 500.0),
            WorkoutSessionSummary(id = "old", startedAtUtc = at(monday.minusDays(1)), totalVolumeKg = 7_000.0),
        )
        val runs = listOf(Run(startedAtUtc = at(wednesday, 7), distanceMeters = 5_100.0))

        val week = TrainingWeek.of(wednesday, zone, workouts, runs)

        assertEquals(2, week.workouts)
        assertEquals(21_000.0, week.liftedKg, 0.0)
        assertEquals(5_100.0, week.runMeters, 0.0)
        assertTrue(week.days[0].lifted)
        assertFalse(week.days[0].ran)
        assertTrue(week.days[2].lifted && week.days[2].ran)
        assertFalse(week.days[1].trained)
    }

    @Test
    fun aWorkoutCountsOnTheDayItFinished() {
        val sundayNight = LocalDate.of(2026, 10, 4).atTime(23, 30).toInstant(zone).toEpochMilli()
        val mondayMorning = at(LocalDate.of(2026, 10, 5), 0)

        val week = TrainingWeek.of(
            wednesday,
            zone,
            listOf(WorkoutSessionSummary(id = "late", startedAtUtc = sundayNight, completedDateUtc = mondayMorning)),
            emptyList(),
        )

        assertTrue(week.days[0].lifted)
        assertEquals(1, week.workouts)
    }
}
