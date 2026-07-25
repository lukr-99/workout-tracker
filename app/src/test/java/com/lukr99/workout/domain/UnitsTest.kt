package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Sanity test wiring up `gradlew test` against the pure `domain/` layer. Real analytics/estimates
 * coverage arrives in Phase 1.
 */
class UnitsTest {

    @Test
    fun kgToLb_convertsAKnownValue() {
        assertEquals(220.462, Units.kgToLb(100.0), 0.001)
    }

    @Test
    fun lbToKg_isInverseOfKgToLb() {
        val kg = 82.5
        assertEquals(kg, Units.lbToKg(Units.kgToLb(kg)), 1e-9)
    }
}
