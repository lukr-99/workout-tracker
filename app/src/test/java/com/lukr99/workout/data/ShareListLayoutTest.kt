package com.lukr99.workout.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareListLayoutTest {

    @Test
    fun capacity_countsWholeRowsOnly() {
        assertEquals(9, ShareListLayout.capacity(top = 556f, bottom = 1270f, rowHeight = 76f))
        assertEquals(0, ShareListLayout.capacity(top = 100f, bottom = 100f, rowHeight = 76f))
        assertEquals(0, ShareListLayout.capacity(top = 0f, bottom = 100f, rowHeight = 0f))
    }

    @Test
    fun everythingShowsWhenItFits() {
        assertEquals(5, ShareListLayout.visibleCount(total = 5, capacity = 5))
        assertEquals(0, ShareListLayout.overflow(total = 5, capacity = 5))
    }

    @Test
    fun overflowGivesTheLastRowToMore() {
        assertEquals(4, ShareListLayout.visibleCount(total = 9, capacity = 5))
        assertEquals(5, ShareListLayout.overflow(total = 9, capacity = 5))
    }

    @Test
    fun tinyCapacityShowsOnlyTheMoreLine() {
        assertEquals(0, ShareListLayout.visibleCount(total = 3, capacity = 1))
        assertEquals(3, ShareListLayout.overflow(total = 3, capacity = 1))
        assertEquals(0, ShareListLayout.visibleCount(total = 3, capacity = 0))
    }
}
