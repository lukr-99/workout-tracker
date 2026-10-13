package com.lukr99.workout.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareListLayoutTest {

    private fun plan(vararg hasSets: Boolean, rowsTop: Float = 500f) = ShareListLayout.plan(
        hasSets = hasSets.toList(),
        rowsTop = rowsTop,
        bottomSpace = 80f,
        compactHeight = 76f,
        expandedHeight = 116f,
        minHeight = 1080,
        maxHeight = 1350,
    )

    @Test
    fun aShortListExpandsAndTheCardStaysSquare() {
        val plan = plan(true, true, false)
        assertTrue(plan.expanded)
        assertEquals(3, plan.shown)
        assertEquals(0, plan.overflow)
        assertEquals(listOf(116f, 116f, 76f), plan.rowHeights)
        assertEquals(1080, plan.cardHeight)
        assertEquals(192f, plan.slack, 0.001f) // 1080 - (500 + 308 + 80)
    }

    @Test
    fun theCardGrowsWithTheListUpTo4By5() {
        // 6 expanded rows: 500 + 696 + 80 = 1276.
        val plan = plan(true, true, true, true, true, true)
        assertTrue(plan.expanded)
        assertEquals(1276, plan.cardHeight)
        assertEquals(0f, plan.slack, 0.001f)
    }

    @Test
    fun rowsCollapseToOneLineWhenExpandedRowsWouldNotFit() {
        // Space is 1350 - 80 - 500 = 770: 7 expanded rows (812) do not fit, 7 compact rows (532) do.
        val plan = plan(*BooleanArray(7) { true })
        assertFalse(plan.expanded)
        assertEquals(7, plan.shown)
        assertEquals(1112, plan.cardHeight)
    }

    @Test
    fun overflowGivesTheLastRowToMore() {
        // 770 of space holds 10 compact rows; with 12 rows, 9 show and the 10th slot says "+3 more".
        val plan = plan(*BooleanArray(12) { true })
        assertFalse(plan.expanded)
        assertEquals(9, plan.shown)
        assertEquals(3, plan.overflow)
        assertEquals(1340, plan.cardHeight) // 500 + 9 * 76 + 76 + 80
    }

    @Test
    fun rowsWithoutSetsNeverExpand() {
        val plan = plan(false, false)
        assertFalse(plan.expanded)
        assertEquals(listOf(76f, 76f), plan.rowHeights)
    }

    @Test
    fun anEmptyListMakesASquareCard() {
        val plan = plan()
        assertEquals(0, plan.shown)
        assertEquals(1080, plan.cardHeight)
    }

    @Test
    fun visibleCountKeepsRoomForTheMoreRow() {
        assertEquals(3, ShareListLayout.visibleCount(listOf(100f, 100f, 100f), space = 300f, moreHeight = 50f))
        assertEquals(2, ShareListLayout.visibleCount(listOf(100f, 100f, 100f, 100f), space = 300f, moreHeight = 50f))
        assertEquals(0, ShareListLayout.visibleCount(listOf(100f, 100f), space = 120f, moreHeight = 50f))
    }
}
