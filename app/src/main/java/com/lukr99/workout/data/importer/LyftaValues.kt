package com.lukr99.workout.data.importer

import com.lukr99.workout.domain.SetType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

/** Reads single Lyfta CSV cells. Each parser returns null for a value it cannot read. */
internal object LyftaValues {

    fun parseSetType(value: String?): SetType? = when (
        value?.trim()?.uppercase()?.replace('-', '_')?.replace(' ', '_')
    ) {
        null, "" -> SetType.Normal
        "NORMAL", "NORMAL_SET", "WORKING_SET" -> SetType.Normal
        "WARMUP", "WARM_UP", "WARMUP_SET", "WARM_UP_SET" -> SetType.Warmup
        "DROP", "DROP_SET" -> SetType.Drop
        "FAILURE", "FAILURE_SET", "TO_FAILURE" -> SetType.Failure
        "NEGATIVE", "NEGATIVE_SET", "NEGATIVE_REPS_SET" -> SetType.Negative
        "BACK_OFF", "BACKOFF", "BACK_OFF_SET", "BACKOFF_SET" -> SetType.BackOff
        else -> null
    }

    /**
     * Reads a date cell as epoch millis. A value with an offset or `Z` (`2024-05-01T10:00:00Z`,
     * `2024-05-01T10:00:00+02:00`, seconds optional) is exact. A local value
     * (`yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd HH:mm`, or ISO `yyyy-MM-ddTHH:mm[:ss[.fraction]]`) is read
     * in [zone]. Anything else gives null.
     */
    fun parseDate(value: String, zone: TimeZone): Long? {
        OFFSET_DATE.matchEntire(value)?.let { match ->
            val local = localDateTime(match) ?: return null
            val offset = offsetOf(match.groupValues[OFFSET_GROUP]) ?: return null
            return local.toInstant(offset).toEpochMilliseconds()
        }
        val match = LOCAL_DATE.matchEntire(value.trim()) ?: return null
        // A space allows no fraction. Only the ISO form with a T does.
        if (match.groupValues[SEPARATOR_GROUP] == " " && match.groupValues[FRACTION_GROUP].isNotEmpty()) return null
        // The space formats were read leniently: a day past the end of the month ("2023-02-30")
        // moved back to the last day of it. The ISO form with a T never did.
        val local = localDateTime(match, clampDay = match.groupValues[SEPARATOR_GROUP] == " ") ?: return null
        return local.toInstant(zone).toEpochMilliseconds()
    }

    private fun localDateTime(match: MatchResult, clampDay: Boolean = false): LocalDateTime? {
        val g = match.groupValues
        return try {
            val year = g[1].toInt()
            val month = g[2].toInt()
            val day = g[3].toInt()
            LocalDateTime(
                year = year,
                monthNumber = month,
                dayOfMonth = if (clampDay && month in 1..12 && day in 29..31) minOf(day, daysInMonth(year, month)) else day,
                hour = g[5].toInt(),
                minute = g[6].toInt(),
                second = g[7].ifEmpty { "0" }.toInt(),
                nanosecond = g[FRACTION_GROUP].padEnd(9, '0').toInt(),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        return when (month) {
            2 -> if (leap) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    private fun offsetOf(text: String): UtcOffset? {
        if (text == "Z" || text == "z") return UtcOffset.ZERO
        val sign = if (text[0] == '-') -1 else 1
        val parts = text.substring(1).split(':').map(String::toInt)
        val seconds = parts[0] * 3_600 + parts[1] * 60 + (parts.getOrNull(2) ?: 0)
        if (parts[0] > 18 || parts[1] > 59 || (parts.getOrNull(2) ?: 0) > 59) return null
        return try {
            UtcOffset(seconds = sign * seconds)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun parseDuration(value: String?): Int? {
        val clean = value?.trim()?.takeUnless { it.isBlank() || it.equals("null", true) } ?: return null
        clean.toIntOrNull()?.let { return it }
        val pieces = clean.split(':').mapNotNull(String::toIntOrNull)
        if (pieces.size != clean.count { it == ':' } + 1) return null
        return when (pieces.size) {
            3 -> pieces[0] * 3_600 + pieces[1] * 60 + pieces[2]
            2 -> pieces[0] * 60 + pieces[1]
            1 -> pieces[0]
            else -> null
        }
    }

    fun parseNumber(value: String?): Double? {
        val clean = value?.trim()?.takeUnless { it.isBlank() || it.equals("null", true) } ?: return null
        return clean.removeSuffix("kg").removeSuffix("lbs").removeSuffix("lb")
            .trim().replace(',', '.').toDoubleOrNull()
    }

    // Groups: 1 year, 2 month, 3 day, 4 separator, 5 hour, 6 minute, 7 second, 8 fraction, 9 offset.
    private const val SEPARATOR_GROUP = 4
    private const val FRACTION_GROUP = 8
    private const val OFFSET_GROUP = 9
    private val OFFSET_DATE = Regex(
        "(\\d{4})-(\\d{2})-(\\d{2})([Tt])(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,9}))?)?" +
            "([Zz]|[+-]\\d{2}:\\d{2}(?::\\d{2})?)",
    )
    private val LOCAL_DATE = Regex(
        "(\\d{4})-(\\d{2})-(\\d{2})([Tt ])(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,9}))?)?",
    )
}
