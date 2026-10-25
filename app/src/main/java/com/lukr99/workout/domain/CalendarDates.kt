package com.lukr99.workout.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/*
 * Calendar helpers over kotlinx-datetime, so domain/ and the shared UI run on every platform
 * (the web edition has no java.time). Timestamps stay epoch-millis UTC `Long`s, as everywhere else.
 */

/** The current time in epoch millis. */
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** The calendar day [this] epoch-millis instant falls on in [zone]. */
fun Long.dateIn(zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(zone).date

/** The first instant of this day in [zone], in epoch millis. */
fun LocalDate.startMillis(zone: TimeZone): Long = atStartOfDayIn(zone).toEpochMilliseconds()

fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)

fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)

/** The Monday on or before this day. */
fun LocalDate.mondayOfWeek(): LocalDate = minusDays(dayOfWeek.isoDayNumber - 1)

/** The ISO-8601 week-based year of this day (the year its week's Thursday is in). */
val LocalDate.isoWeekYear: Int get() = plusDays(4 - dayOfWeek.isoDayNumber).year

/** The ISO-8601 week number of this day, 1 to 53. */
val LocalDate.isoWeekNumber: Int get() = (plusDays(4 - dayOfWeek.isoDayNumber).dayOfYear - 1) / 7 + 1
