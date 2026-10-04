package com.lukr99.workout.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterVersionTest {

    @Test
    fun devSuffix_marksADevelopmentBuild() {
        assertTrue(AppUpdater.isDevelopmentVersion("2.5.2-dev"))
        assertTrue(AppUpdater.isDevelopmentVersion(" 2.6.0-DEV "))
        assertFalse(AppUpdater.isDevelopmentVersion("2.5.2"))
        assertFalse(AppUpdater.isDevelopmentVersion("v2.5.2"))
    }

    @Test
    fun isNewer_comparesDottedNumbers() {
        assertTrue(AppUpdater.isNewer("2.6.0", "2.5.2"))
        assertTrue(AppUpdater.isNewer("v2.10.0", "2.9.9"))
        assertFalse(AppUpdater.isNewer("2.5.2", "2.5.2"))
        assertFalse(AppUpdater.isNewer("2.5.1", "2.5.2"))
    }
}
