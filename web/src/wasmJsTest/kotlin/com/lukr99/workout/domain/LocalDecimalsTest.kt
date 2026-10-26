package com.lukr99.workout.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDecimalsTest {
    @Test
    fun writesTheRequestedNumberOfDecimals() {
        assertEquals("0.00", localDecimal(0.0, 2))
        assertEquals("12.50", localDecimal(12.5, 2))
        assertEquals("100", localDecimal(100.0, 0))
        assertEquals("7.0", localDecimal(7.0, 1))
        assertEquals("1234567.891", localDecimal(1234567.8912, 3))
    }

    @Test
    fun alwaysUsesAPoint() {
        assertEquals("3.14", localDecimal(3.14159, 2))
    }

    @Test
    fun roundsHalfUpOnTheDecimalForm() {
        // Java's formatter gives these, and the web edition follows it.
        assertEquals("0.2", localDecimal(0.15, 1))
        assertEquals("1.01", localDecimal(1.005, 2))
        assertEquals("13", localDecimal(12.5, 0))
        assertEquals("0.1", localDecimal(0.05, 1))
        assertEquals("0.00", localDecimal(0.004, 2))
        assertEquals("10.0", localDecimal(9.96, 1))
        assertEquals("1000", localDecimal(999.5, 0))
    }

    @Test
    fun keepsTheSignOfNegativeValues() {
        assertEquals("-2.50", localDecimal(-2.5, 2))
        assertEquals("-0.00", localDecimal(-0.001, 2))
        assertEquals("-3", localDecimal(-2.5, 0))
    }

    @Test
    fun handlesTinyAndHugeNumbers() {
        assertEquals("0.00", localDecimal(4.0e-10, 2))
        assertEquals("1000000000000000000000.0", localDecimal(1.0e21, 1))
    }

    @Test
    fun namesNonFiniteValues() {
        assertEquals("NaN", localDecimal(Double.NaN, 1))
        assertEquals("Infinity", localDecimal(Double.POSITIVE_INFINITY, 1))
        assertEquals("-Infinity", localDecimal(Double.NEGATIVE_INFINITY, 1))
    }
}
