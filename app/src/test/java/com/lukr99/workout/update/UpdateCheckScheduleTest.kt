package com.lukr99.workout.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckScheduleTest {
    private val day = UpdateCheckSchedule.INTERVAL_MS
    private val now = 100 * day

    @Test
    fun checksOnceADayWhenOn() {
        assertTrue(UpdateCheckSchedule.isDue(enabled = true, lastCheckUtc = null, nowUtc = now))
        assertTrue(UpdateCheckSchedule.isDue(enabled = true, lastCheckUtc = now - day, nowUtc = now))
        assertFalse(UpdateCheckSchedule.isDue(enabled = true, lastCheckUtc = now - day + 1, nowUtc = now))
    }

    @Test
    fun neverWhenOff() {
        assertFalse(UpdateCheckSchedule.isDue(enabled = false, lastCheckUtc = null, nowUtc = now))
    }

    @Test
    fun aClockThatWentBackDoesNotBlockChecksForever() {
        assertTrue(UpdateCheckSchedule.isDue(enabled = true, lastCheckUtc = now + day * 30, nowUtc = now))
    }
}
