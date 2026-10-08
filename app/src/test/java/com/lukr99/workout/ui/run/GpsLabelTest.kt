package com.lukr99.workout.ui.run

import org.junit.Assert.assertEquals
import org.junit.Test

class GpsLabelTest {

    @Test
    fun withoutPermissionTheCardDescribesTheRun() {
        assertEquals("GPS route, pace and splits", gpsLabel(permitted = false, hasFix = true, accuracyM = 4.0))
    }

    @Test
    fun withPermissionItSaysHowTheFixLooks() {
        assertEquals("Looking for GPS…", gpsLabel(permitted = true, hasFix = false, accuracyM = null))
        assertEquals("GPS ready · ±4 m", gpsLabel(permitted = true, hasFix = true, accuracyM = 4.2))
        assertEquals("GPS ready · ±20 m", gpsLabel(permitted = true, hasFix = true, accuracyM = 20.0))
        assertEquals("Weak GPS · ±35 m. Open sky helps.", gpsLabel(permitted = true, hasFix = true, accuracyM = 35.0))
        assertEquals("GPS ready", gpsLabel(permitted = true, hasFix = true, accuracyM = null))
    }
}
