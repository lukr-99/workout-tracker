package com.lukr99.workout.domain.run

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCueTest {

    private val km = Pace.METERS_PER_KM
    private val mile = Pace.METERS_PER_MILE

    @Test
    fun noMark_whenStepStaysWithinOneSplit() {
        assertEquals(emptyList<Int>(), SplitCue.crossedMarks(100.0, 400.0, km))
        assertEquals(emptyList<Int>(), SplitCue.crossedMarks(1001.0, 1400.0, km))
    }

    @Test
    fun singleMark_whenBoundaryFallsInsideStep() {
        assertEquals(listOf(1), SplitCue.crossedMarks(980.0, 1010.0, km))
        assertEquals(listOf(2), SplitCue.crossedMarks(1990.0, 2005.0, km))
    }

    @Test
    fun markAtExactCurr_countsButAtExactPrevDoesNot() {
        // Half-open (prev, curr]: hitting the mark exactly announces it once.
        assertEquals(listOf(1), SplitCue.crossedMarks(500.0, 1000.0, km))
        // Starting exactly on a mark must not re-announce it on the next step.
        assertEquals(emptyList<Int>(), SplitCue.crossedMarks(1000.0, 1500.0, km))
        assertEquals(listOf(2), SplitCue.crossedMarks(1000.0, 2000.0, km))
    }

    @Test
    fun multipleMarks_whenOneStepJumpsSeveralSplits() {
        // A long GPS gap can straddle more than one boundary — all crossed marks, in order.
        assertEquals(listOf(1, 2, 3), SplitCue.crossedMarks(500.0, 3200.0, km))
    }

    @Test
    fun unitAware_milesUseMileBoundaries() {
        // ~1.5 mi: crosses only mile 1, not km-style marks.
        assertEquals(listOf(1), SplitCue.crossedMarks(0.0, 1.5 * mile, mile))
        assertEquals(listOf(1, 2), SplitCue.crossedMarks(0.0, 2.0 * mile, mile))
    }

    @Test
    fun replayingWholeRun_announcesEachSplitExactlyOnce() {
        // Simulate the live loop feeding (prev, curr) each second for a 3 km run.
        val announced = ArrayList<Int>()
        var prev = 0.0
        var d = 0.0
        while (d <= 3050.0) { // run a little past 3 km so the 3rd mark is actually crossed
            val curr = d
            announced += SplitCue.crossedMarks(prev, curr, km)
            prev = curr
            d += 7.3 // arbitrary per-tick advance
        }
        assertEquals(listOf(1, 2, 3), announced)
    }

    @Test
    fun degenerateInputs_areSafe() {
        assertEquals(emptyList<Int>(), SplitCue.crossedMarks(500.0, 500.0, km)) // no advance
        assertEquals(emptyList<Int>(), SplitCue.crossedMarks(500.0, 400.0, km)) // backward
        assertEquals(emptyList<Int>(), SplitCue.crossedMarks(0.0, 1000.0, 0.0)) // bad split length
    }

    @Test
    fun spokenMessage_isUnitAwareAndPaceOptional() {
        assertEquals("1 kilometre. 5 minutes per kilometre.", SplitCue.spokenMessage(1, imperial = false, avgPaceSecPerUnit = 300.0))
        assertEquals("3 miles. 8 minutes 22 seconds per mile.", SplitCue.spokenMessage(3, imperial = true, avgPaceSecPerUnit = 502.0))
        assertEquals("2 kilometres.", SplitCue.spokenMessage(2, imperial = false, avgPaceSecPerUnit = 0.0))
    }
}
