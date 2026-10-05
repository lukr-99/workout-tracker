package com.lukr99.workout.data

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalExerciseMergeTest {

    private val synced = Exercise(
        id = "synced",
        name = "Remote Row",
        primaryBodyPart = "Back",
        source = ExerciseSource.Synced,
        externalSourceId = "wger:row",
        notes = "Keep my note",
    )

    @Test
    fun aNewExternalExerciseIsAddedAsSynced() {
        val plan = ExternalExerciseMerge.plan(
            existing = emptyList(),
            incoming = listOf(Exercise(name = " Cable Fly ", externalSourceId = " wger:fly ")),
        )

        assertEquals(ExternalExerciseMergeSummary(added = 1), plan.summary)
        val added = plan.upserts.single()
        assertEquals("Cable Fly", added.name)
        assertEquals("wger:fly", added.externalSourceId)
        assertEquals(ExerciseSource.Synced, added.source)
    }

    @Test
    fun aSyncedRowOnlyGetsBlankFieldsFilled() {
        val plan = ExternalExerciseMerge.plan(
            existing = listOf(synced),
            incoming = listOf(
                synced.copy(
                    name = "Remote rename",
                    notes = "Overwrite attempt",
                    equipment = "Cable",
                    secondaryBodyParts = listOf("Biceps"),
                ),
            ),
        )

        assertEquals(ExternalExerciseMergeSummary(updated = 1), plan.summary)
        val updated = plan.upserts.single()
        assertEquals("Remote Row", updated.name)
        assertEquals("Keep my note", updated.notes)
        assertEquals("Cable", updated.equipment)
        assertEquals(listOf("Biceps"), updated.secondaryBodyParts)
    }

    @Test
    fun anUnchangedSyncedRowIsSkippedWithoutAWrite() {
        val plan = ExternalExerciseMerge.plan(existing = listOf(synced), incoming = listOf(synced))

        assertEquals(ExternalExerciseMergeSummary(skipped = 1), plan.summary)
        assertTrue(plan.upserts.isEmpty())
    }

    @Test
    fun customRowsAndNameCollisionsAreNeverTouched() {
        val custom = Exercise(
            id = "custom",
            name = "My Press",
            source = ExerciseSource.Custom,
            externalSourceId = "wger:protected",
        )

        val plan = ExternalExerciseMerge.plan(
            existing = listOf(custom),
            incoming = listOf(
                custom.copy(name = "Overwrite attempt"),
                Exercise(name = "my press", externalSourceId = "wger:other"),
            ),
        )

        assertEquals(ExternalExerciseMergeSummary(skipped = 2), plan.summary)
        assertTrue(plan.upserts.isEmpty())
    }

    @Test
    fun rowsWithoutAnIdOrSeenTwiceInOneBatchAreSkipped() {
        val plan = ExternalExerciseMerge.plan(
            existing = emptyList(),
            incoming = listOf(
                Exercise(name = "No id"),
                Exercise(name = "First", externalSourceId = "wger:dup"),
                Exercise(name = "Second", externalSourceId = "WGER:DUP"),
            ),
        )

        assertEquals(ExternalExerciseMergeSummary(added = 1, skipped = 2), plan.summary)
        assertEquals(listOf("First"), plan.upserts.map { it.name })
    }

    @Test
    fun stepsKeptInNotesBeforeVersion8AreNotShownTwice() {
        val old = synced.copy(notes = "Pull to the hip.")

        val plan = ExternalExerciseMerge.plan(
            existing = listOf(old),
            incoming = listOf(old.copy(instructions = "Pull to the hip.", equipment = "Cable")),
        )

        assertEquals("", plan.upserts.single().instructions)
    }
}
