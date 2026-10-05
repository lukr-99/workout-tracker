package com.lukr99.workout.data.importer

import com.lukr99.workout.domain.SetType
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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

    fun parseDate(value: String, zone: ZoneId): Long? {
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        DATE_FORMATS.forEach { formatter ->
            try {
                return LocalDateTime.parse(value.trim(), formatter).atZone(zone).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Try the next supported format.
            }
        }
        return null
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

    private val DATE_FORMATS = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    )
}
