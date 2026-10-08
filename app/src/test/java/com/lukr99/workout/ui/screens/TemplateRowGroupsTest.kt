package com.lukr99.workout.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRowGroupsTest {

    private fun row(id: String, group: Int? = null) = TemplateRow(id, id, id, "", "", supersetGroup = group)

    @Test
    fun joiningStartsANewGroupOrTakesTheGroupAbove() {
        val rows = listOf(row("a"), row("b"), row("c", group = 4), row("d"))

        assertEquals(listOf(5, 5, 4, null), TemplateRowGroups.joinPrevious(rows, 1).map { it.supersetGroup })
        assertEquals(listOf(null, null, 4, 4), TemplateRowGroups.joinPrevious(rows, 3).map { it.supersetGroup })
        assertEquals(rows, TemplateRowGroups.joinPrevious(rows, 0))
    }

    @Test
    fun leavingAGroupOfTwoDissolvesIt() {
        val rows = listOf(row("a", 1), row("b", 1), row("c", 2), row("d", 2), row("e", 2))

        assertEquals(listOf(null, null, 2, 2, 2), TemplateRowGroups.leave(rows, 1).map { it.supersetGroup })
        assertEquals(listOf(1, 1, 2, null, 2), TemplateRowGroups.leave(rows, 3).map { it.supersetGroup })
    }

    @Test
    fun joinedMeansSameGroupAsTheRowAbove() {
        val rows = listOf(row("a", 1), row("b", 1), row("c"))

        assertFalse(TemplateRowGroups.joinedToPrevious(rows, 0))
        assertTrue(TemplateRowGroups.joinedToPrevious(rows, 1))
        assertFalse(TemplateRowGroups.joinedToPrevious(rows, 2))
    }

    @Test
    fun ungroupDissolvesTheWholeSuperset() {
        val rows = listOf(row("a", 1), row("b", 1), row("c", 1), row("d", 2), row("e", 2))

        assertEquals(listOf(null, null, null, 2, 2), TemplateRowGroups.ungroup(rows, 1).map { it.supersetGroup })
    }

    @Test
    fun tidyDropsARowDraggedAwayFromItsGroup() {
        // "c" was dragged from the superset with "a" and "b" to the end.
        val rows = listOf(row("a", 1), row("b", 1), row("d"), row("c", 1))

        assertEquals(listOf(1, 1, null, null), TemplateRowGroups.tidy(rows).map { it.supersetGroup })
    }

    @Test
    fun tidyDissolvesAGroupLeftWithOneRow() {
        val rows = listOf(row("a", 1), row("x"), row("b", 1))

        assertEquals(listOf(null, null, null), TemplateRowGroups.tidy(rows).map { it.supersetGroup })
    }

    @Test
    fun tidySplitsAGroupCutInTwo() {
        // A row dropped into the middle of a superset of four leaves two pairs.
        val rows = listOf(row("a", 3), row("b", 3), row("x"), row("c", 3), row("d", 3))

        assertEquals(listOf(3, 3, null, 4, 4), TemplateRowGroups.tidy(rows).map { it.supersetGroup })
    }

    @Test
    fun tidyLeavesIntactGroupsAlone() {
        val rows = listOf(row("a", 1), row("b", 1), row("c"), row("d", 2), row("e", 2))

        assertEquals(rows, TemplateRowGroups.tidy(rows))
    }

    @Test
    fun repsShiftMovesTheWholeRange() {
        val ranged = row("a").copy(repsMin = 6, repsMax = 8)
        assertEquals(7 to 9, ranged.withRepsShifted(1).let { it.repsMin to it.repsMax })
        assertEquals(5 to 7, ranged.withRepsShifted(-1).let { it.repsMin to it.repsMax })

        val one = row("a").copy(repsMin = 1, repsMax = 3)
        assertEquals(1 to 3, one.withRepsShifted(-1).let { it.repsMin to it.repsMax })

        val empty = row("a")
        assertEquals(TemplateRow.START_REPS to TemplateRow.START_REPS, empty.withRepsShifted(1).let { it.repsMin to it.repsMax })
        assertEquals(empty, empty.withRepsShifted(-1))

        val maxOnly = row("a").copy(repsMax = 10)
        assertEquals(11 to 11, maxOnly.withRepsShifted(1).let { it.repsMin to it.repsMax })
    }
}
