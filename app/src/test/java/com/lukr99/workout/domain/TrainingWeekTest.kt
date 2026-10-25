package com.lukr99.workout.domain

import com.lukr99.workout.domain.run.Run
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingWeekTest {

    private val zone = TimeZone.UTC
    private val wednesday = LocalDate(2026, 10, 7)

    private fun at(date: LocalDate, hour: Int = 18) = date.atTime(hour, 0).toInstant(zone).toEpochMilliseconds()

    @Test
    fun theWeekRunsMondayToSundayAndMarksToday() {
        val week = TrainingWeek.of(wednesday, zone, emptyList(), emptyList())

        assertEquals(LocalDate(2026, 10, 5), week.days.first().date)
        assertEquals(LocalDate(2026, 10, 11), week.days.last().date)
        assertEquals(listOf(false, false, true, false, false, false, false), week.days.map { it.isToday })
    }

    @Test
    fun finishedWorkoutsAndRunsMarkTheirDaysAndAddUp() {
        val monday = LocalDate(2026, 10, 5)
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
        val sundayNight = LocalDate(2026, 10, 4).atTime(23, 30).toInstant(zone).toEpochMilliseconds()
        val mondayMorning = at(LocalDate(2026, 10, 5), 0)

        val week = TrainingWeek.of(
            wednesday,
            zone,
            listOf(WorkoutSessionSummary(id = "late", startedAtUtc = sundayNight, completedDateUtc = mondayMorning)),
            emptyList(),
        )

        assertTrue(week.days[0].lifted)
        assertEquals(1, week.workouts)
    }

    @Test
    fun eachDayListsItsWorkoutsAndRunsOldestFirst() {
        val monday = LocalDate(2026, 10, 5)
        val workouts = listOf(
            WorkoutSessionSummary(id = "evening", name = "Pull", startedAtUtc = at(wednesday, 18), completedDateUtc = at(wednesday, 19)),
            WorkoutSessionSummary(id = "solo", name = "Push", startedAtUtc = at(monday), completedDateUtc = at(monday)),
            WorkoutSessionSummary(id = "live", startedAtUtc = at(wednesday), status = WorkoutSessionStatus.Active),
        )
        val runs = listOf(Run(id = "morning", startedAtUtc = at(wednesday, 7), notes = "Easy 5k\nfelt good"), Run(id = "plain", startedAtUtc = at(wednesday, 12)))

        val week = TrainingWeek.of(wednesday, zone, workouts, runs)

        assertEquals(listOf(DayActivity("solo", isRun = false, name = "Push", atUtc = at(monday))), week.days[0].activities)
        assertEquals(listOf("morning", "plain", "evening"), week.days[2].activities.map { it.id })
        assertEquals(listOf("Easy 5k", "Run", "Pull"), week.days[2].activities.map { it.name })
        assertTrue(week.days[1].activities.isEmpty())
    }
}
