package com.lukr99.workout.data.importer

import com.lukr99.workout.domain.SetType
import java.time.ZoneOffset
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
        assertEquals(0L, LyftaValues.parseDate("1970-01-01T00:00:00Z", ZoneOffset.UTC))
        assertEquals(3_600_000L, LyftaValues.parseDate("1970-01-01 02:00:00", ZoneOffset.ofHours(1)))
        assertNull(LyftaValues.parseDate("yesterday", ZoneOffset.UTC))
    }
}
