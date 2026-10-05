package com.lukr99.workout.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScrollTest {

    @Test
    fun navigationNeedsFourSections() {
        assertFalse(SettingsScroll.showNavigation(3))
        assertTrue(SettingsScroll.showNavigation(4))
    }

    @Test
    fun theCurrentSectionIsTheLastWhoseTopPassedTheLine() {
        // Section 0 starts at the top, 1 at 60 (above the 80 line), 2 at 500, 3 below the screen.
        assertEquals(1, SettingsScroll.currentSection(listOf(0, 60, 500, null), firstVisible = 0, lineY = 80, atBottom = false))
        assertEquals(0, SettingsScroll.currentSection(listOf(0, 300, 900, null), firstVisible = 0, lineY = 80, atBottom = false))
    }

    @Test
    fun sectionsScrolledAboveTheViewHavePassed() {
        assertEquals(2, SettingsScroll.currentSection(listOf(null, null, 40, 400), firstVisible = 2, lineY = 80, atBottom = false))
        assertEquals(1, SettingsScroll.currentSection(listOf(null, -200, 300, 900), firstVisible = 1, lineY = 80, atBottom = false))
    }

    @Test
    fun theBottomOfThePageMakesTheLastSectionCurrent() {
        assertEquals(4, SettingsScroll.currentSection(listOf(null, null, -50, 200, 600), firstVisible = 2, lineY = 80, atBottom = true))
    }
}
