package com.lukr99.workout.ui.components

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/*
 * Day and weekday names. Each platform supplies this file: Android uses java.time with the phone's
 * locale; the web edition writes English names itself, so the page reads the same on every browser.
 */

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** [date] written in [style], e.g. "Mon 13 Oct". */
fun localDateText(date: LocalDate, style: DateStyle): String = formatDatePattern(date, style.pattern)

/** The weekday's name, in full ("Monday") or as one letter ("M"). */
fun weekdayName(day: DayOfWeek, narrow: Boolean): String {
    val full = dayNames[day.ordinal]
    return if (narrow) full.substring(0, 1) else full
}

/**
 * [date] written with a java.time style [pattern]. Only the letters the app's [DateStyle] patterns
 * use are understood: EEEE and EEE (weekday), d (day of month), MMMM and MMM (month) and yyyy
 * (year). Spaces and any other character are copied as they are.
 */
internal fun formatDatePattern(date: LocalDate, pattern: String): String = buildString {
    var i = 0
    while (i < pattern.length) {
        val letter = pattern[i]
        var end = i
        while (end < pattern.length && pattern[end] == letter) end++
        val count = end - i
        when (letter) {
            'E' -> append(if (count >= 4) dayNames[date.dayOfWeek.ordinal] else dayNames[date.dayOfWeek.ordinal].take(3))
            'M' -> append(if (count >= 4) monthNames[date.monthNumber - 1] else monthNames[date.monthNumber - 1].take(3))
            'd' -> append(date.dayOfMonth.toString().padStart(count, '0'))
            'y' -> append(if (count == 2) (date.year % 100).toString().padStart(2, '0') else date.year.toString().padStart(count, '0'))
            else -> append(pattern, i, end)
        }
        i = end
    }
}
