package com.lukr99.workout.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetEntryStateTest {

    private val start = SetEntryState.of(weightDisplay = 82.5, reps = 8)

    @Test
    fun theFirstKeyReplacesAndLaterKeysAppend() {
        val typed = start.press("1").press("0").press("0")

        assertEquals("100", typed.weight)
        assertFalse(typed.fresh)
    }

    @Test
    fun pickingAFieldStartsFreshTyping() {
        val reps = start.select(SetField.Reps).press("1").press("2")

        assertEquals("12", reps.reps)
        assertEquals("82.5", reps.weight)
    }

    @Test
    fun weightTakesOneDecimalAndRepsTakeNone() {
        assertEquals("82.5", start.press("8").press("2").press(".").press("5").press("5").weight)
        assertEquals("0.", start.press(".").weight)
        assertEquals("8", start.select(SetField.Reps).press(".").reps)
    }

    @Test
    fun backspaceClearsAFreshValueAndDeletesOtherwise() {
        assertEquals("0", start.press(SetEntryState.BACKSPACE).weight)
        assertEquals("8", start.press("8").press("5").press(SetEntryState.BACKSPACE).weight)
    }

    @Test
    fun stepsChangeTheValueAndNeverGoBelowZero() {
        assertEquals("85", start.step(2.5).weight)
        assertTrue(start.step(2.5).fresh)
        assertEquals("0", start.copy(weight = "1").step(-2.5).weight)
        assertEquals("9", start.select(SetField.Reps).step(1.0).reps)
    }

    @Test
    fun copyingAnotherSetTakesBothValues() {
        val copied = start.copyFrom(weightDisplay = 80.0, repsCount = 6)

        assertEquals("80", copied.weight)
        assertEquals(6, copied.repsValue)
    }

    @Test
    fun valuesAreLimitedToFiveCharacters() {
        val long = start.press("1").press("2").press("3").press("4").press("5").press("6")

        assertEquals("12345", long.weight)
    }
}
