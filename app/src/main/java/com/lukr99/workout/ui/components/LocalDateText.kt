package com.lukr99.workout.ui.components

import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

/*
 * Day and weekday names in the device's language. Each platform supplies this file: on Android it
 * is java.time with the phone's locale, as the app always had; the web edition's is under web/.
 */

private val formatters = DateStyle.entries.associateWith { DateTimeFormatter.ofPattern(it.pattern, Locale.getDefault()) }

/** [date] written in [style], e.g. "Mon 13 Oct". */
fun localDateText(date: LocalDate, style: DateStyle): String = date.toJavaLocalDate().format(formatters.getValue(style))

/** The weekday's name, in full ("Monday") or as one letter ("M"). */
fun weekdayName(day: DayOfWeek, narrow: Boolean): String =
    day.getDisplayName(if (narrow) TextStyle.NARROW else TextStyle.FULL, Locale.getDefault())
