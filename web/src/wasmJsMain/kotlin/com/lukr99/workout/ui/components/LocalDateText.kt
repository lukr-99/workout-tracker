package com.lukr99.workout.ui.components

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

fun localDateText(date: LocalDate, style: DateStyle): String = date.toString()

fun weekdayName(day: DayOfWeek, narrow: Boolean): String = day.name
