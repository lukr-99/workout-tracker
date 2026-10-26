package com.lukr99.workout.ui.components

import com.lukr99.workout.domain.Units
import com.lukr99.workout.domain.WeightDisplayUnit
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.domain.dateIn
import com.lukr99.workout.domain.localDecimal
import com.lukr99.workout.domain.nowMillis
import com.lukr99.workout.domain.padded
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * UI display formatting — the one place weights get converted for display and timestamps become
 * human strings. Storage stays metric/epoch-millis (03-data-model.md "store raw"); this rounds and
 * localizes only at the edge.
 */
object Format {

    fun unitLabel(units: UnitSystem): String = if (units == UnitSystem.Imperial) "lb" else "kg"

    fun entryUnits(override: WeightDisplayUnit?, fallback: UnitSystem): UnitSystem = when (override) {
        WeightDisplayUnit.Kilograms -> UnitSystem.Metric
        WeightDisplayUnit.Pounds -> UnitSystem.Imperial
        null -> fallback
    }

    /** Convert a stored-kg weight into the display unit and round for display (no unit suffix). */
    fun weight(kg: Double, units: UnitSystem): String {
        val value = if (units == UnitSystem.Imperial) Units.kgToLb(kg) else kg
        return Units.formatWeight(value)
    }

    /** Weight with its unit suffix, e.g. "100 kg" / "220 lb". */
    fun weightWithUnit(kg: Double, units: UnitSystem): String = "${weight(kg, units)} ${unitLabel(units)}"

    /** Convert display-unit input back to stored kg. */
    fun toKg(displayValue: Double, units: UnitSystem): Double =
        if (units == UnitSystem.Imperial) Units.lbToKg(displayValue) else displayValue

    /** Convert stored kg to the display unit as a raw Double (for stepper editing). */
    fun toDisplay(kg: Double, units: UnitSystem): Double =
        if (units == UnitSystem.Imperial) Units.kgToLb(kg) else kg

    fun volume(kg: Double, units: UnitSystem): String {
        val value = if (units == UnitSystem.Imperial) Units.kgToLb(kg) else kg
        return when {
            value >= 100_000 -> "${localDecimal(value / 1_000_000, 1)}M"
            value >= 10_000 -> "${localDecimal(value / 1_000, 0)}k"
            value >= 1_000 -> "${localDecimal(value / 1_000, 1)}k"
            else -> Units.formatWeight(value)
        }
    }

    fun duration(totalSeconds: Long): String = Units.formatDuration(totalSeconds)

    /** Distance in km/mi with a unit suffix, e.g. "5.2 km" / "3.2 mi". Storage stays metres. */
    fun distance(meters: Double, units: UnitSystem): String {
        return if (units == UnitSystem.Imperial) {
            "${localDecimal(meters / 1_609.344, 2)} mi"
        } else {
            "${localDecimal(meters / 1_000.0, 2)} km"
        }
    }

    fun clock(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return "${s / 60}:${padded(s % 60, 2)}"
    }

    fun date(millis: Long): String = localDateText(localDay(millis), DateStyle.Day)

    fun fullDate(millis: Long): String = localDateText(localDay(millis), DateStyle.DayYear)

    fun shortDate(millis: Long): String = localDateText(localDay(millis), DateStyle.ShortDay)

    /** The weekday and date in full, e.g. "Monday 13 Oct". */
    fun longDay(date: LocalDate): String = localDateText(date, DateStyle.LongDay)

    /** The local time of day, e.g. "18:05". */
    fun clockTime(millis: Long): String {
        val time = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).time
        return "${padded(time.hour, 2)}:${padded(time.minute, 2)}"
    }

    /** "Today" / "Yesterday" / "3 days ago" / a date for older sessions. */
    fun relativeDay(millis: Long, nowMillis: Long = nowMillis()): String {
        val day = localDay(millis)
        val today = localDay(nowMillis)
        return when (val days = today.toEpochDays() - day.toEpochDays()) {
            0 -> "Today"
            1 -> "Yesterday"
            in 2..6 -> "$days days ago"
            else -> localDateText(day, if (day.year == today.year) DateStyle.ShortDay else DateStyle.DayYear)
        }
    }

    fun today(): LocalDate = localDay(nowMillis())

    private fun localDay(millis: Long): LocalDate = millis.dateIn(TimeZone.currentSystemDefault())
}
