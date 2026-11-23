package com.lukr99.workout.ui.components

import com.lukr99.workout.ui.i18n.AppLanguage
import com.lukr99.workout.ui.i18n.Language
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/*
 * Day and weekday names. Each platform supplies this file: Android uses java.time with the phone's
 * locale; the web edition writes the names itself (English or Czech, from AppLanguage), so the page reads the same on every browser.
 */

private val englishMonths = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

// In the form used after a day number: "13. října".
private val czechMonths = listOf(
    "ledna", "února", "března", "dubna", "května", "června",
    "července", "srpna", "září", "října", "listopadu", "prosince",
)
private val czechMonthsShort = listOf("led", "úno", "bře", "dub", "kvě", "čvn", "čvc", "srp", "zář", "říj", "lis", "pro")

private val englishDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val czechDays = listOf("pondělí", "úterý", "středa", "čtvrtek", "pátek", "sobota", "neděle")
private val czechDaysShort = listOf("po", "út", "st", "čt", "pá", "so", "ne")

private val czech get() = AppLanguage.current == Language.Czech
private fun dayName(index: Int, full: Boolean): String = when {
    czech -> if (full) czechDays[index] else czechDaysShort[index]
    else -> if (full) englishDays[index] else englishDays[index].take(3)
}
private fun monthName(index: Int, full: Boolean): String = when {
    czech -> if (full) czechMonths[index] else czechMonthsShort[index]
    else -> if (full) englishMonths[index] else englishMonths[index].take(3)
}

/** [date] written in [style], e.g. "Mon 13 Oct". */
fun localDateText(date: LocalDate, style: DateStyle): String = formatDatePattern(date, style.patternFor(AppLanguage.current))

/** The weekday's name, in full ("Monday") or as one letter ("M"). */
fun weekdayName(day: DayOfWeek, narrow: Boolean): String {
    val full = dayName(day.ordinal, full = true)
    return if (narrow) full.substring(0, 1).uppercase() else full
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
            'E' -> append(dayName(date.dayOfWeek.ordinal, full = count >= 4))
            'M' -> append(monthName(date.monthNumber - 1, full = count >= 4))
            'd' -> append(date.dayOfMonth.toString().padStart(count, '0'))
            'y' -> append(if (count == 2) (date.year % 100).toString().padStart(2, '0') else date.year.toString().padStart(count, '0'))
            else -> append(pattern, i, end)
        }
        i = end
    }
}
