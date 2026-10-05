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
}
