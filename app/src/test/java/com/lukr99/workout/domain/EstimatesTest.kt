package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimatesTest {

    @Test
    fun epley_knownValues() {
        assertEquals(100.0, Estimates.epley(100.0, 1), 1e-9) // 1 rep = the load
        assertEquals(100.0 * (1 + 5.0 / 30), Estimates.epley(100.0, 5), 1e-9)
        assertEquals(0.0, Estimates.epley(100.0, 0), 1e-9)
    }

    @Test
    fun brzycki_knownValuesAndBounds() {
        assertEquals(100.0, Estimates.brzycki(100.0, 1), 1e-9)
        assertEquals(100.0 * 36.0 / (37 - 5), Estimates.brzycki(100.0, 5), 1e-9)
        assertEquals(0.0, Estimates.brzycki(100.0, 37), 1e-9) // division-by-zero guard
    }

    @Test
    fun bestEpley_ignoresWarmupsAndTakesMax() {
        val sets = listOf(
            StrengthSet(reps = 10, weightKg = 40.0, isWarmup = true),
            StrengthSet(reps = 5, weightKg = 100.0),
            StrengthSet(reps = 3, weightKg = 110.0),
        )
        val expected = maxOf(Estimates.epley(100.0, 5), Estimates.epley(110.0, 3))
        assertEquals(expected, Estimates.bestEpley(sets), 1e-9)
    }

    @Test
    fun volumeAndReps() {
        val sets = listOf(
            StrengthSet(reps = 10, weightKg = 40.0, isWarmup = true),
            StrengthSet(reps = 5, weightKg = 100.0),
        )
        assertEquals(400.0 + 500.0, Estimates.volume(sets), 1e-9)
        assertEquals(500.0, Estimates.volume(sets, includeWarmups = false), 1e-9)
        assertEquals(15, Estimates.totalReps(sets))
        assertEquals(5, Estimates.totalReps(sets, includeWarmups = false))
    }
}
