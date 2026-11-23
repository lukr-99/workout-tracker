package com.lukr99.workout.ui.components

import com.lukr99.workout.ui.i18n.AppLanguage
import com.lukr99.workout.ui.i18n.Language
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

/*
 * Day and weekday names in the language Ember is set to. Each platform supplies this file: on Android it
 * is java.time with the phone's locale, as the app always had; the web edition's is under web/.
 */

/** One formatter per style and language, built on first use. */
private val formatters = java.util.concurrent.ConcurrentHashMap<Pair<DateStyle, Language>, DateTimeFormatter>()

/** [date] written in [style], e.g. "Mon 13 Oct". */
fun localDateText(date: LocalDate, style: DateStyle): String = date.toJavaLocalDate().format(
        formatters.getOrPut(style to AppLanguage.current) {
            DateTimeFormatter.ofPattern(style.patternFor(AppLanguage.current), Locale.forLanguageTag(AppLanguage.current.code))
        },
    )

/** The weekday's name, in full ("Monday") or as one letter ("M"). */
fun weekdayName(day: DayOfWeek, narrow: Boolean): String =
    day.getDisplayName(if (narrow) TextStyle.NARROW else TextStyle.FULL, Locale.forLanguageTag(AppLanguage.current.code))
