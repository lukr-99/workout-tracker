package com.lukr99.workout.web

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareCardTextTest {
    @Test
    fun smallVolumesAreGroupedByThousands() {
        assertEquals("0", ShareCardText.compactNumber(0.0))
        assertEquals("950", ShareCardText.compactNumber(949.6))
        assertEquals("9,876", ShareCardText.compactNumber(9_876.4))
    }

    @Test
    fun largeVolumesAreShortened() {
        assertEquals("12.3k", ShareCardText.compactNumber(12_345.0))
        assertEquals("10.0k", ShareCardText.compactNumber(10_000.0))
        assertEquals("123k", ShareCardText.compactNumber(123_456.0))
    }

    @Test
    fun durationsShowHoursOnlyWhenThereAreSome() {
        assertEquals("0m", ShareCardText.formatDuration(20))
        assertEquals("45m", ShareCardText.formatDuration(2_700))
        assertEquals("1h 5m", ShareCardText.formatDuration(3_900))
    }
}
