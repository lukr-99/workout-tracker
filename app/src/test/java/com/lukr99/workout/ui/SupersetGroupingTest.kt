package com.lukr99.workout.ui

import com.lukr99.workout.domain.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SupersetGroupingTest {

    @Test
    fun joiningAndUnjoiningPairUpdatesBothEntries() {
        val entries = entries("a", "b", "c")

        val joined = toggleSupersetBoundary(entries, "b")
        assertEquals(listOf(1, 1, null), joined.map(WorkoutEntry::supersetGroup))

        val unjoined = toggleSupersetBoundary(joined, "b")
        assertEquals(listOf(null, null, null), unjoined.map(WorkoutEntry::supersetGroup))
    }

    @Test
    fun joiningAdjacentRunsMergesThem() {
        val entries = entries("a", "b", "c", "d").mapIndexed { index, entry ->
            entry.copy(supersetGroup = if (index < 2) 3 else 8)
        }

        val joined = toggleSupersetBoundary(entries, "c")

        assertEquals(listOf(3, 3, 3, 3), joined.map(WorkoutEntry::supersetGroup))
    }

    @Test
    fun removingMiddleBoundarySplitsLongRunAndClearsSingleton() {
        val entries = entries("a", "b", "c").map { it.copy(supersetGroup = 4) }

        val split = toggleSupersetBoundary(entries, "b")

        assertEquals(listOf(null, 5, 5), split.map(WorkoutEntry::supersetGroup))
    }

    @Test
    fun normalizationClearsNonConsecutiveSingletons() {
        val entries = entries("a", "b", "c").mapIndexed { index, entry ->
            entry.copy(supersetGroup = if (index == 1) null else 2)
        }

        assertEquals(
            listOf(null, null, null),
            normalizeSupersetGroups(entries).map(WorkoutEntry::supersetGroup),
        )
    }

    @Test
    fun editorCanExtendRemoveAndClearAGroup() {
        val grouped = entries("a", "b", "c", "d", "e").mapIndexed { index, entry ->
            entry.copy(supersetGroup = 7.takeIf { index in 1..3 })
        }

        val extended = extendSupersetGroup(grouped, groupId = 7, before = true)
        assertEquals(listOf(7, 7, 7, 7, null), extended.map(WorkoutEntry::supersetGroup))

        val removed = removeFromSupersetGroup(extended, entryId = "b")
        assertEquals(listOf(null, null, 7, 7, null), removed.map(WorkoutEntry::supersetGroup))

        val cleared = clearSupersetGroup(extended, groupId = 7)
        assertEquals(listOf(null, null, null, null, null), cleared.map(WorkoutEntry::supersetGroup))
    }

    private fun entries(vararg ids: String): List<WorkoutEntry> =
        ids.mapIndexed { index, id -> WorkoutEntry(id = id, sortOrder = index) }
}
