package com.lukr99.workout.domain

import java.time.temporal.IsoFields
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The portable replacements for String.format, java.time week fields, UUID and java.net.URI give the
 * same answers the JVM APIs gave, so the Android app does not change.
 */
class PortableTextTest {

    @Test
    fun formatFixedMatchesStringFormat() {
        val values = listOf(
            0.0, -0.0, 0.04, -0.04, 0.05, 0.5, 1.0, 1.25, 1.35, 2.5, 2.675, 9.995, 99.95, 999.9999,
            1234.5678, 0.000001234, 1e-7, 12_345_678.9, 1e16, -3.14159, 100.0 / 3, 2.0 / 3, 7.45, 0.125,
        )
        for (v in values) {
            for (d in 0..6) {
                assertEquals("$v with $d decimals", String.format(Locale.ROOT, "%.${d}f", v), formatFixed(v, d))
            }
        }
    }

    @Test
    fun paddedMatchesZeroPadFormat() {
        for (v in listOf(0, 5, 42, 123, 2026, 12345, -7)) {
            assertEquals(String.format(Locale.ROOT, "%04d", v), padded(v, 4))
            assertEquals(String.format(Locale.ROOT, "%02d", v), padded(v, 2))
        }
    }

    @Test
    fun isoWeekMatchesJavaTime() {
        var date = LocalDate(2019, 12, 1)
        val end = LocalDate(2027, 2, 1)
        while (date <= end) {
            val java = date.toJavaLocalDate()
            assertEquals("$date year", java.get(IsoFields.WEEK_BASED_YEAR), date.isoWeekYear)
            assertEquals("$date week", java.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), date.isoWeekNumber)
            assertEquals("$date monday", java.minusDays((java.dayOfWeek.value - 1).toLong()), date.mondayOfWeek().toJavaLocalDate())
            date = date.plusDays(1)
        }
    }

    @Test
    fun toRadiansMatchesMath() {
        for (deg in listOf(0.0, 1.0, 45.0, 49.1951, -122.4194, 180.0, 359.99)) {
            assertEquals(Math.toRadians(deg), deg.toRadians(), 0.0)
        }
    }

    @Test
    fun newIdIsThirtyTwoLowercaseHexChars() {
        val ids = List(50) { newId() }
        ids.forEach { assertTrue(it, Regex("^[0-9a-f]{32}$").matches(it)) }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun guideLinkKeepsWebAddresses() {
        assertEquals("https://youtu.be/abc", GuideLink.normalize("youtu.be/abc"))
        assertEquals("https://www.youtube.com/watch?v=x1", GuideLink.normalize("  https://www.youtube.com/watch?v=x1 "))
        assertEquals("http://example.com:8080/a#b", GuideLink.normalize("http://example.com:8080/a#b"))
        assertEquals("https://user@example.com/x", GuideLink.normalize("https://user@example.com/x"))
        assertEquals("HTTPS://Example.COM", GuideLink.normalize("HTTPS://Example.COM"))
    }

    @Test
    fun guideLinkRejectsWhatUriRejected() {
        listOf(
            "", "   ", "ftp://example.com", "javascript://example.com", "https://localhost/x",
            "https://exa mple.com", "https://example.com/a b", "https://ex_ample.com", "https://",
            "https://-bad.com", "https://example.com/<x>", "notaurl",
        ).forEach { assertEquals(it, null, GuideLink.normalize(it)) }
        assertTrue(GuideLink.isAcceptable(null))
        assertFalse(GuideLink.isAcceptable("ftp://example.com"))
    }
}
