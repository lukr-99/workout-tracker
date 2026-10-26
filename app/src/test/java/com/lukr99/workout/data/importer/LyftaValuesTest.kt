package com.lukr99.workout.data.importer

import com.lukr99.workout.domain.SetType
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyftaValuesTest {

    @Test
    fun durationsReadSecondsAndClockTimes() {
        assertEquals(90, LyftaValues.parseDuration("90"))
        assertEquals(90, LyftaValues.parseDuration("1:30"))
        assertEquals(3_723, LyftaValues.parseDuration("1:02:03"))
        assertNull(LyftaValues.parseDuration("null"))
        assertNull(LyftaValues.parseDuration("1:xx"))
    }

    @Test
    fun numbersAcceptUnitsAndDecimalCommas() {
        assertEquals(62.5, LyftaValues.parseNumber(" 62,5kg ")!!, 0.0)
        assertEquals(135.0, LyftaValues.parseNumber("135 lbs")!!, 0.0)
        assertNull(LyftaValues.parseNumber(""))
    }

    @Test
    fun setTypesAcceptCommonSpellings() {
        assertEquals(SetType.Warmup, LyftaValues.parseSetType("warm-up set"))
        assertEquals(SetType.BackOff, LyftaValues.parseSetType("Back off"))
        assertEquals(SetType.Normal, LyftaValues.parseSetType(null))
        assertNull(LyftaValues.parseSetType("cluster"))
    }

    @Test
    fun datesReadInstantsAndLocalTimesInTheGivenZone() {
        assertEquals(0L, LyftaValues.parseDate("1970-01-01T00:00:00Z", TimeZone.UTC))
        assertEquals(3_600_000L, LyftaValues.parseDate("1970-01-01 02:00:00", TimeZone.of("+01:00")))
        assertNull(LyftaValues.parseDate("yesterday", TimeZone.UTC))
    }

    /** The parser as it was written with java.time, kept here to prove the rewrite reads the same. */
    private fun legacyParseDate(value: String, zone: ZoneId): Long? {
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        ).forEach { formatter ->
            try {
                return LocalDateTime.parse(value.trim(), formatter).atZone(zone).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Try the next format.
            } catch (_: DateTimeException) {
                // Out-of-range value.
            }
        }
        return null
    }

    @Test
    fun everyFormatTheOldParserReadGivesTheSameInstant() {
        val values = listOf(
            "2024-05-01T10:00:00Z",
            "2024-05-01T10:00:00.5Z",
            "2024-05-01T10:00:00.123Z",
            "2024-05-01T10:00:00.123456789Z",
            "2024-05-01T10:00:00+02:00",
            "2024-05-01T10:00:00-05:30",
            "2024-05-01T10:00+02:00",
            "2024-05-01T10:00:00.250+01:00",
            "1969-12-31T23:59:59.999Z",
            "1960-03-04 08:15:00",
            "2024-05-01 10:00:00",
            "2024-05-01 10:00",
            "  2024-05-01 10:00:00  ",
            "2024-05-01T10:00",
            "2024-05-01T10:00:30",
            "2024-05-01T10:00:30.75",
            "2024-05-01t10:00:30",
            "2024-03-31 02:30:00",
            "2024-10-27 02:30:00",
            "2024-02-29 12:00:00",
        )
        val invalid = listOf(
            "",
            "yesterday",
            "2024-05-01",
            "2024-5-1 10:00",
            "2023-02-29 12:00:00",
            "2024-13-01 10:00:00",
            "2024-05-01 25:00:00",
            "2024-05-01 10:61:00",
            "2024-05-01 10:00:00.5",
            "2024-05-01T10:00:00 Z",
            " 2024-05-01T10:00:00Z",
            "2024-05-01T10:00:00+0200",
            "01/05/2024 10:00",
            "2024-05-01 10:00:00 PM",
        )
        val zones = listOf("UTC", "Europe/Prague", "America/New_York", "Asia/Kolkata", "Pacific/Chatham", "+05:45")
        for (zoneId in zones) {
            for (value in values + invalid) {
                assertEquals(
                    "'$value' in $zoneId",
                    legacyParseDate(value, ZoneId.of(zoneId)),
                    LyftaValues.parseDate(value, TimeZone.of(zoneId)),
                )
            }
        }
    }

    @Test
    fun localTimesInTheDstGapAndOverlapMatchTheOldParser() {
        val prague = TimeZone.of("Europe/Prague")
        // 2024-03-31 02:30 does not exist in Prague. 2024-10-27 02:30 happens twice.
        assertEquals(1_711_848_600_000L, LyftaValues.parseDate("2024-03-31 02:30:00", prague))
        assertEquals(1_729_989_000_000L, LyftaValues.parseDate("2024-10-27 02:30:00", prague))
    }
}
