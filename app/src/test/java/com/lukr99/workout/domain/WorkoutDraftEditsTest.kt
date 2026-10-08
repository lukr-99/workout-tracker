package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDraftEditsTest {

    private val bench = WorkoutEntry(
        id = "bench",
        exerciseId = "ex-bench",
        sortOrder = 0,
        strengthSets = listOf(StrengthSet(id = "s1", setNumber = 1, reps = 5, weightKg = 100.0)),
    )
    private val row = WorkoutEntry(id = "row", exerciseId = "ex-row", sortOrder = 1)
    private val workout = WorkoutSession(id = "w", startedAtUtc = 0, entries = listOf(bench, row))

    @Test
    fun movingAnExerciseRenumbersAndStopsAtTheEnds() {
        val moved = workout.withEntryMoved("row", up = true)

        assertEquals(listOf("row", "bench"), moved.entries.map { it.id })
        assertEquals(listOf(0, 1), moved.entries.map { it.sortOrder })
        assertSame(workout, workout.withEntryMoved("bench", up = true))
        assertSame(workout, workout.withEntryMoved("missing", up = false))
    }

    @Test
    fun aNewSetCopiesTheLastSet() {
        val entry = bench.withSetAdded()

        val added = entry.strengthSets.last()
        assertEquals(2, added.setNumber)
        assertEquals(5, added.reps)
        assertEquals(100.0, added.weightKg, 0.0)
        assertEquals("bench", added.workoutEntryId)
    }

    @Test
    fun markingASetDoneStartsTheExerciseOnce() {
        val done = workout.withSetDone("bench", "s1", doneAt = 1_000)
        val entry = done.entries.first()

        assertEquals(1_000L, entry.startedAtUtc)
        assertEquals(1_000L, entry.strengthSets.single().performedAtUtc)
        assertEquals(setOf("s1"), done.setIdsMarkedDone)

        val undone = done.withSetDone("bench", "s1", doneAt = null).entries.first()
        assertNull(undone.strengthSets.single().performedAtUtc)
        assertEquals(1_000L, undone.startedAtUtc)
    }

    @Test
    fun finishingAnExerciseFallsBackToItsFirstDoneSet() {
        val finished = workout
            .withSetDone("bench", "s1", doneAt = 500)
            .withEntry("bench") { it.copy(startedAtUtc = null) }
            .withEntryFinished("bench", now = 900)
            .entries.first()

        assertEquals(500L, finished.startedAtUtc)
        assertEquals(900L, finished.completedAtUtc)
    }

    @Test
    fun togglingATagKeepsTheOldSetTypeInStep() {
        val warmup = bench.strengthSets.single().withTagToggled(SetTag.Warmup)

        assertEquals(setOf(SetTag.Warmup), warmup.tags)
        assertTrue(warmup.isWarmup)
        assertEquals(SetType.Warmup, warmup.setType)

        val cleared = warmup.withTagToggled(SetTag.Warmup)
        assertTrue(cleared.tags.isEmpty())
        assertEquals(SetType.Normal, cleared.setType)
    }

    @Test
    fun theWeightUnitFlipsFromTheDefaultWhenNoneIsSet() {
        assertEquals(WeightDisplayUnit.Kilograms, bench.withWeightUnitToggled(poundsByDefault = true).weightUnitOverride)
        assertEquals(WeightDisplayUnit.Pounds, bench.withWeightUnitToggled(poundsByDefault = false).weightUnitOverride)
    }

    @Test
    fun completingDropsEmptyExercisesAndClosesTheRest() {
        val cardio = WorkoutEntry(id = "bike", entryType = ExerciseCategory.Cardio, cardioData = CardioEntryData())
        val closed = workout.copy(entries = listOf(bench.copy(completedAtUtc = 700), row, cardio))

        val completed = closed.completedAt(now = 2_000)

        assertEquals(WorkoutSessionStatus.Completed, completed.status)
        assertEquals(2_000L, completed.endedAtUtc)
        assertEquals(listOf("bench", "bike"), completed.entries.map { it.id })
        assertEquals(700L, completed.entries.first().completedAtUtc)
        assertEquals(2_000L, completed.entries.last().completedAtUtc)
        assertEquals(2_000L, completed.entries.last().startedAtUtc)
    }

    @Test
    fun lastSetsComeFromTheNewestFinishedWorkout() {
        fun finished(at: Long, reps: Int) = WorkoutSession(
            status = WorkoutSessionStatus.Completed,
            startedAtUtc = at,
            completedDateUtc = at,
            entries = listOf(bench.copy(strengthSets = listOf(StrengthSet(reps = reps, weightKg = 60.0)))),
        )
        val live = WorkoutSession(
            status = WorkoutSessionStatus.Active,
            startedAtUtc = 9_000,
            entries = listOf(bench.copy(strengthSets = listOf(StrengthSet(reps = 1)))),
        )

        val sets = listOf(finished(1_000, reps = 8), finished(5_000, reps = 10), live).lastSetsFor("ex-bench")

        assertEquals(listOf(10), sets.map { it.reps })
        assertEquals(listOf(1), sets.map { it.setNumber })
        assertTrue(listOf(live).lastSetsFor("ex-bench").isEmpty())
        assertTrue(listOf(finished(1_000, reps = 8)).lastSetsFor("").isEmpty())
    }

    @Test
    fun addedExercisesGoToTheEndAndCanFormANewSuperset() {
        val grouped = workout.copy(entries = listOf(bench.copy(supersetGroup = 2), row.copy(supersetGroup = 2)))
        val fly = WorkoutEntry(id = "fly")
        val dips = WorkoutEntry(id = "dips")

        val superset = grouped.withEntriesAdded(listOf(fly, dips), asSuperset = true)
        assertEquals(listOf("bench", "row", "fly", "dips"), superset.entries.map { it.id })
        assertEquals(listOf(2, 3), superset.entries.map { it.sortOrder }.takeLast(2))
        assertEquals(listOf(3, 3), superset.entries.takeLast(2).map { it.supersetGroup })
        assertTrue(superset.entries.takeLast(2).all { it.workoutSessionId == "w" })

        val single = workout.withEntriesAdded(listOf(fly), asSuperset = true)
        assertNull(single.entries.last().supersetGroup)
    }

    @Test
    fun duplicatingASetPutsAnUnloggedCopyRightAfterIt() {
        val entry = WorkoutEntry(
            id = "e",
            strengthSets = listOf(
                StrengthSet(id = "a", setNumber = 1, reps = 10, weightKg = 60.0),
                StrengthSet(
                    id = "b", setNumber = 2, reps = 8, weightKg = 80.0, performedAtUtc = 5L, notes = "Tight",
                    isPr = true, rir = 1.0, rpe = 9.0, tags = setOf(SetTag.ToFailure),
                ),
                StrengthSet(id = "c", setNumber = 3, reps = 6, weightKg = 80.0),
            ),
        )

        val sets = entry.withSetDuplicated("b").strengthSets

        assertEquals(listOf("a", "b", null, "c"), sets.map { it.id.takeIf { id -> id in setOf("a", "b", "c") } })
        assertEquals(listOf(1, 2, 3, 4), sets.map { it.setNumber })
        val copy = sets[2]
        assertEquals(8 to 80.0, copy.reps to copy.weightKg)
        assertEquals(setOf(SetTag.ToFailure), copy.tags)
        assertEquals(null, copy.performedAtUtc)
        assertEquals("", copy.notes)
        assertEquals(false, copy.isPr)
        assertEquals(null, copy.rir)
        assertEquals(entry, entry.withSetDuplicated("missing"))
    }

    @Test
    fun repsInReserveSetsRirAndRpeTogether() {
        val set = StrengthSet().withRepsInReserve(1)
        assertEquals(1.0, set.rir)
        assertEquals(9.0, set.rpe)
        assertEquals(1, set.repsInReserve)

        assertEquals(4.0, StrengthSet().withRepsInReserve(7).rir)
        val cleared = set.withRepsInReserve(null)
        assertEquals(null, cleared.rir)
        assertEquals(null, cleared.rpe)
        assertEquals(null, cleared.repsInReserve)
    }

    @Test
    fun repsInReserveReadsOlderRpeOnlySets() {
        assertEquals(2, StrengthSet(rpe = 8.0).repsInReserve)
        // RPE 8.5 is one to two reps left; it rounds up to 2.
        assertEquals(2, StrengthSet(rpe = 8.5).repsInReserve)
        assertEquals(4, StrengthSet(rpe = 5.0).repsInReserve)
        assertEquals(0, StrengthSet(rir = 0.0).repsInReserve)
    }

    private val mixed = WorkoutSession(
        entries = listOf(
            WorkoutEntry(
                id = "bench",
                strengthSets = listOf(
                    StrengthSet(id = "b1", setNumber = 1, reps = 8, weightKg = 80.0, performedAtUtc = 5L),
                    StrengthSet(id = "b2", setNumber = 2, reps = 8, weightKg = 80.0),
                    StrengthSet(id = "b3", setNumber = 3, reps = 0),
                ),
            ),
            WorkoutEntry(id = "fly", strengthSets = listOf(StrengthSet(id = "f1", reps = 12, weightKg = 15.0))),
        ),
    )

    @Test
    fun untickedSetsAreCounted() {
        assertEquals(3, mixed.untickedSetCount)
    }

    @Test
    fun leavingUntickedSetsOutKeepsOnlyTheTickedOnes() {
        val finished = mixed.withUntickedSets(countThem = false, now = 9L).completedAt(9L)

        assertEquals(listOf("bench"), finished.entries.map { it.id })
        assertEquals(listOf("b1"), finished.entries.single().strengthSets.map { it.id })
    }

    @Test
    fun countingUntickedSetsTicksThoseWithRepsAndDropsBlankOnes() {
        val finished = mixed.withUntickedSets(countThem = true, now = 9L)

        assertEquals(listOf("b1", "b2"), finished.entries[0].strengthSets.map { it.id })
        assertEquals(listOf(5L, 9L), finished.entries[0].strengthSets.map { it.performedAtUtc })
        assertEquals(listOf(1, 2), finished.entries[0].strengthSets.map { it.setNumber })
        assertEquals(listOf(9L), finished.entries[1].strengthSets.map { it.performedAtUtc })
    }
}
