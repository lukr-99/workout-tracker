package com.lukr99.workout.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressContextTest {

    private val zone = TimeZone.UTC

    // Thursday 8 October 2026, noon.
    private val now = LocalDate(2026, 10, 8).atTime(12, 0).toInstant(zone).toEpochMilliseconds()

    private fun day(date: LocalDate) = date.atTime(10, 0).toInstant(zone).toEpochMilliseconds()

    private fun workout(date: LocalDate, kg: Double, prs: Int = 0, status: WorkoutSessionStatus = WorkoutSessionStatus.Completed) = WorkoutSession(
        startedAtUtc = day(date),
        completedDateUtc = day(date),
        status = status,
        entries = listOf(
            WorkoutEntry(strengthSets = listOf(StrengthSet(reps = 1, weightKg = kg)) + List(prs) { StrengthSet(isPr = true) }),
        ),
    )

    @Test
    fun windowsCountFromNow() {
        val sessions = listOf(
            workout(LocalDate(2026, 10, 6), kg = 100.0, prs = 1), // this week, this month
            workout(LocalDate(2026, 10, 1), kg = 200.0), // this month, last 4 weeks
            workout(LocalDate(2026, 9, 20), kg = 300.0, prs = 2), // last 4 weeks, last 30 days
            workout(LocalDate(2026, 8, 25), kg = 500.0), // the 4 weeks before
            workout(LocalDate(2026, 10, 7), kg = 900.0, status = WorkoutSessionStatus.Discarded),
        )

        val context = ProgressContext.of(sessions, now, zone)

        assertEquals(2, context.workoutsThisMonth)
        assertEquals(600.0, context.volumeLast4WeeksKg, 0.001)
        assertEquals(500.0, context.volumePrevious4WeeksKg, 0.001)
        assertEquals(20, context.volumeChangePercent)
        assertEquals(3, context.prSetsLast30Days)
        assertEquals(100.0, context.volumeThisWeekKg, 0.001)
    }

    @Test
    fun noChangeWithoutAnEarlierWindow() {
        assertNull(ProgressContext.of(listOf(workout(LocalDate(2026, 10, 6), kg = 100.0)), now, zone).volumeChangePercent)
    }

    @Test
    fun bestStreakIsTheLongestRunOfWeeks() {
        val dates = listOf(
            LocalDate(2026, 1, 5), LocalDate(2026, 1, 13), LocalDate(2026, 1, 21), // three weeks running
            LocalDate(2026, 3, 2), // alone
            LocalDate(2026, 3, 16), LocalDate(2026, 3, 18), LocalDate(2026, 3, 23), // two weeks
        ).map(::day)

        assertEquals(3, Analytics.bestWeeklyStreak(dates))
        assertEquals(0, Analytics.bestWeeklyStreak(emptyList()))
    }
}
