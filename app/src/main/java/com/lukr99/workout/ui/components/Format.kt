package com.lukr99.workout.ui.components

import com.lukr99.workout.domain.Units
import com.lukr99.workout.settings.UnitSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * UI display formatting — the one place weights get converted for display and timestamps become
 * human strings. Storage stays metric/epoch-millis (03-data-model.md "store raw"); this rounds and
 * localizes only at the edge.
 */
object Format {

    private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val dayYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val shortDayFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun unitLabel(units: UnitSystem): String = if (units == UnitSystem.Imperial) "lb" else "kg"

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
            value >= 100_000 -> "%.1fM".format(value / 1_000_000)
            value >= 10_000 -> "%.0fk".format(value / 1_000)
            value >= 1_000 -> "%.1fk".format(value / 1_000)
            else -> Units.formatWeight(value)
        }
    }

    fun duration(totalSeconds: Long): String = Units.formatDuration(totalSeconds)

    /** Distance in km/mi with a unit suffix, e.g. "5.2 km" / "3.2 mi". Storage stays metres. */
    fun distance(meters: Double, units: UnitSystem): String {
        return if (units == UnitSystem.Imperial) {
            "%.2f mi".format(meters / 1_609.344)
        } else {
            "%.2f km".format(meters / 1_000.0)
        }
    }

    fun clock(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun date(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormatter)

    fun fullDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dayYearFormatter)

    fun shortDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(shortDayFormatter)

    /** "Today" / "Yesterday" / "3 days ago" / a date for older sessions. */
    fun relativeDay(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val zone = ZoneId.systemDefault()
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return when (val days = ChronoUnit.DAYS.between(day, today)) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2..6 -> "$days days ago"
            else -> if (day.year == today.year) day.format(shortDayFormatter) else day.format(dayYearFormatter)
        }
    }

    fun today(): LocalDate = LocalDate.now()
}
