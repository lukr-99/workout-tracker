package com.lukr99.workout.ui.components

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTextTest {
    // 13 October 2026 is a Tuesday.
    private val date = LocalDate(2026, 10, 13)

    @Test
    fun writesEveryStyleInEnglish() {
        assertEquals("Tue 13 Oct", localDateText(date, DateStyle.Day))
        assertEquals("13 Oct 2026", localDateText(date, DateStyle.DayYear))
        assertEquals("13 Oct", localDateText(date, DateStyle.ShortDay))
        assertEquals("Tuesday 13 Oct", localDateText(date, DateStyle.LongDay))
        assertEquals("Tuesday 13 October", localDateText(date, DateStyle.Header))
    }

    @Test
    fun doesNotPadTheDay() {
        assertEquals("Sun 1 Jan", localDateText(LocalDate(2023, 1, 1), DateStyle.Day))
    }

    @Test
    fun coversEveryMonth() {
        val short = (1..12).map { localDateText(LocalDate(2026, it, 1), DateStyle.ShortDay) }
        assertEquals(
            listOf("1 Jan", "1 Feb", "1 Mar", "1 Apr", "1 May", "1 Jun", "1 Jul", "1 Aug", "1 Sep", "1 Oct", "1 Nov", "1 Dec"),
            short,
        )
        assertEquals("September", formatDatePattern(LocalDate(2026, 9, 5), "MMMM"))
    }

    @Test
    fun copiesOtherCharactersAsTheyAre() {
        assertEquals("Tue, 13 Oct 2026", formatDatePattern(date, "EEE, d MMM yyyy"))
    }

    @Test
    fun namesWeekdaysInFullAndAsOneLetter() {
        assertEquals("Monday", weekdayName(DayOfWeek.MONDAY, narrow = false))
        assertEquals("Sunday", weekdayName(DayOfWeek.SUNDAY, narrow = false))
        assertEquals("M", weekdayName(DayOfWeek.MONDAY, narrow = true))
        assertEquals("T", weekdayName(DayOfWeek.THURSDAY, narrow = true))
    }
}
