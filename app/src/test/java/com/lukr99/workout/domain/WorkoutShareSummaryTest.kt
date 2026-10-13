package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutShareSummaryTest {

    private fun set(kg: Double, reps: Int, warmup: Boolean = false, pr: Boolean = false, performed: Long? = 1L) =
        StrengthSet(weightKg = kg, reps = reps, isWarmup = warmup, isPr = pr, performedAtUtc = performed)

    private fun lift(name: String, order: Int, vararg sets: StrengthSet, override: WeightDisplayUnit? = null) =
        WorkoutEntry(exerciseSnapshotName = name, sortOrder = order, strengthSets = sets.toList(), weightUnitOverride = override)

    private val session = WorkoutSession(
        name = "Push Day",
        entries = listOf(
            lift("Overhead Press", 1, set(40.0, 8), set(42.5, 6, pr = true)),
            lift("Bench Press", 0, set(40.0, 10, warmup = true), set(100.0, 5), set(100.0, 6), set(90.0, 8)),
            WorkoutEntry(
                exerciseSnapshotName = "Treadmill",
                sortOrder = 2,
                entryType = ExerciseCategory.Cardio,
                exerciseSnapshotCategory = ExerciseCategory.Cardio,
                cardioData = CardioEntryData(durationSeconds = 600),
            ),
            lift("Dips", 3, set(0.0, 12)),
            lift("Skipped", 4),
        ),
    )

    @Test
    fun bestSet_isHeaviestWorkingSetWithMoreRepsBreakingTies() {
        val best = WorkoutShareSummary.bestSet(listOf(set(120.0, 3, warmup = true), set(100.0, 5), set(100.0, 6)))
        assertEquals(100.0, best!!.weightKg, 0.0)
        assertEquals(6, best.reps)
    }

    @Test
    fun bestSet_fallsBackToWarmupsWhenNothingElseAndIgnoresZeroReps() {
        assertEquals(60.0, WorkoutShareSummary.bestSet(listOf(set(60.0, 5, warmup = true)))!!.weightKg, 0.0)
        assertNull(WorkoutShareSummary.bestSet(listOf(set(100.0, 0))))
    }

    @Test
    fun formatSet_usesUnitsAndBodyweightReps() {
        assertEquals("100 kg x 5", WorkoutShareSummary.formatSet(set(100.0, 5), imperial = false))
        assertEquals("220.5 lb x 5", WorkoutShareSummary.formatSet(set(100.0, 5), imperial = true))
        assertEquals("12 reps", WorkoutShareSummary.formatSet(set(0.0, 12), imperial = false))
        assertEquals("1 rep", WorkoutShareSummary.formatSet(set(0.0, 1), imperial = false))
        assertEquals("82.5 x 8", WorkoutShareSummary.formatSet(set(82.5, 8), imperial = false, withUnit = false))
    }

    @Test
    fun exerciseLines_followWorkoutOrderAndSkipEmptyExercises() {
        val lines = WorkoutShareSummary.exerciseLines(session, imperial = false)
        assertEquals(
            listOf(
                ShareExerciseLine("Bench Press", "100 kg x 6", hasPr = false, listOf("100 x 5", "100 x 6", "90 x 8")),
                ShareExerciseLine("Overhead Press", "42.5 kg x 6", hasPr = true, listOf("40 x 8", "42.5 x 6")),
                ShareExerciseLine("Treadmill", "10:00", hasPr = false),
                ShareExerciseLine("Dips", "12 reps", hasPr = false, listOf("12 reps")),
            ),
            lines,
        )
    }

    @Test
    fun exerciseLines_respectThePerExerciseUnitOverride() {
        val kgOverride = session.copy(entries = listOf(lift("Squat", 0, set(100.0, 5), override = WeightDisplayUnit.Kilograms)))
        assertEquals("100 kg x 5", WorkoutShareSummary.exerciseLines(kgOverride, imperial = true).single().detail)
    }

    @Test
    fun exerciseLines_countOnlyPerformedSetsWhenAnyWereTicked() {
        val partly = session.copy(entries = listOf(lift("Row", 0, set(80.0, 8), set(120.0, 8, performed = null))))
        assertEquals("80 kg x 8", WorkoutShareSummary.exerciseLines(partly, imperial = false).single().detail)
    }

    @Test
    fun text_listsEverySetAndMarksWarmups() {
        val expected = """
            Push Day
            Tue 13 Oct

            Bench Press
            - 40 kg x 10 (warm-up)
            - 100 kg x 5
            - 100 kg x 6
            - 90 kg x 8

            Overhead Press
            - 40 kg x 8
            - 42.5 kg x 6 PR

            Treadmill
            - 10:00

            Dips
            - 12 reps

            Logged with Ember
        """.trimIndent()
        assertEquals(expected, WorkoutShareSummary.text(session, imperial = false, dateLabel = "Tue 13 Oct"))
    }

    @Test
    fun text_canLeaveWarmupsOut() {
        val text = WorkoutShareSummary.text(session, imperial = false, dateLabel = "d", includeWarmups = false)
        assert("warm-up" !in text)
        assert("- 100 kg x 5" in text)
    }

    @Test
    fun text_treatsTaggedWarmupsAsWarmups() {
        val tagged = session.copy(
            name = "",
            entries = listOf(lift("Curl", 0, StrengthSet(weightKg = 10.0, reps = 12, tags = setOf(SetTag.Warmup)))),
        )
        assertEquals(
            "Workout\nd\n\nCurl\n- 10 kg x 12 (warm-up)\n\nLogged with Ember",
            WorkoutShareSummary.text(tagged, imperial = false, dateLabel = "d"),
        )
    }
}
