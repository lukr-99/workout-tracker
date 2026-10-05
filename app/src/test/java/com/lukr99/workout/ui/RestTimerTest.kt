package com.lukr99.workout.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerTest {

    @Test
    fun countsDownOncePerSecondAndStopsAtZero() = runTest {
        val timer = RestTimer(backgroundScope)

        timer.start(3)
        runCurrent()
        assertEquals(RestState(running = true, remaining = 3, total = 3), timer.state.value)

        advanceTimeBy(1_001)
        assertEquals(2, timer.state.value.remaining)

        advanceTimeBy(2_000)
        assertEquals(0, timer.state.value.remaining)
        assertFalse(timer.state.value.running)
    }

    @Test
    fun addingTimeRaisesTheTotalWhenItGoesPastIt() = runTest {
        val timer = RestTimer(backgroundScope)

        timer.start(10)
        timer.add(15)

        assertEquals(25, timer.state.value.remaining)
        assertEquals(25, timer.state.value.total)
        assertTrue(timer.state.value.running)
    }

    @Test
    fun skippingClearsTheTimer() = runTest {
        val timer = RestTimer(backgroundScope)

        timer.start(60)
        timer.skip()
        advanceTimeBy(5_000)

        assertEquals(RestState(), timer.state.value)
    }
}
