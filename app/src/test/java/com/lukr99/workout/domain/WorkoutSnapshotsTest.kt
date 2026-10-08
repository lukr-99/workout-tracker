package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSnapshotsTest {

    @Test
    fun aNewEntryCopiesTheExerciseAndStartsWithOneSet() {
        val bench = Exercise(id = "bench", name = "Bench Press", primaryBodyPart = "Chest")

        val entry = bench.toNewEntry(sortOrder = 2)

        assertEquals("bench", entry.exerciseId)
        assertEquals("Bench Press", entry.exerciseSnapshotName)
        assertEquals("Chest", entry.exerciseSnapshotPrimaryBodyPart)
        assertEquals(2, entry.sortOrder)
        assertEquals(listOf(1), entry.strengthSets.map { it.setNumber })
        assertNull(entry.cardioData)
    }

    @Test
    fun aCardioEntryStartsWithCardioDataAndNoSets() {
        val bike = Exercise(name = "Bike", category = ExerciseCategory.Cardio)

        val entry = bike.toNewEntry()

        assertTrue(entry.strengthSets.isEmpty())
        assertNotNull(entry.cardioData)
    }

    @Test
    fun templateEntriesFollowTemplateOrderAndKeepNotes() {
        val template = WorkoutTemplate(
            name = "Push",
            exercises = listOf(
                WorkoutTemplateExercise(exerciseId = "b", exerciseName = "Dips", sortOrder = 5),
                WorkoutTemplateExercise(exerciseId = "a", exerciseName = "Bench", sortOrder = 1, notes = "Pause"),
            ),
        )

        val entries = template.toEntries("session")

        assertEquals(listOf("Bench", "Dips"), entries.map { it.exerciseSnapshotName })
        assertEquals(listOf(0, 1), entries.map { it.sortOrder })
        assertEquals("Pause", entries.first().notes)
        assertTrue(entries.all { it.workoutSessionId == "session" })
    }

    @Test
    fun aTemplateFromAWorkoutKeepsCatalogExercisesOnly() {
        val workout = WorkoutSession(
            name = "Monday",
            startedAtUtc = 0,
            entries = listOf(
                WorkoutEntry(exerciseId = "a", exerciseSnapshotName = "Bench", sortOrder = 0),
                WorkoutEntry(exerciseId = "", exerciseSnapshotName = "Free text", sortOrder = 1),
            ),
        )

        assertEquals("Monday Copy", workout.toTemplate().name)
        val named = workout.toTemplate(" Push day ")
        assertEquals("Push day", named.name)
        assertEquals(listOf("Bench"), named.exercises.map { it.exerciseName })
    }

    @Test
    fun repeatingAWorkoutCopiesItsSetsButNothingIsLogged() {
        val past = WorkoutSession(
            id = "old",
            name = "Push day",
            notes = "Felt tired",
            entries = listOf(
                WorkoutEntry(
                    id = "e2", exerciseId = "dips", exerciseSnapshotName = "Dips", sortOrder = 1, notes = "Shoulder ok",
                    supersetGroup = 1,
                    strengthSets = listOf(StrengthSet(id = "s9", setNumber = 1, reps = 10, performedAtUtc = 5L, rpe = 8.0, isPr = true)),
                ),
                WorkoutEntry(
                    id = "e1", exerciseId = "bench", exerciseSnapshotName = "Bench", sortOrder = 0,
                    strengthSets = listOf(
                        StrengthSet(setNumber = 2, reps = 8, weightKg = 80.0, performedAtUtc = 9L),
                        StrengthSet(setNumber = 1, reps = 10, weightKg = 60.0, tags = setOf(SetTag.Warmup)),
                    ),
                ),
                WorkoutEntry(exerciseId = "", exerciseSnapshotName = "Deleted exercise"),
            ),
        )
        val today = WorkoutSession(id = "new", name = DEFAULT_WORKOUT_NAME)

        val repeated = today.repeating(past)

        assertEquals("Push day", repeated.name)
        assertEquals("", repeated.notes)
        assertEquals(listOf("bench", "dips"), repeated.entries.map { it.exerciseId })
        assertTrue(repeated.entries.all { it.workoutSessionId == "new" && it.notes.isEmpty() && it.id !in setOf("e1", "e2") })
        val bench = repeated.entries[0].strengthSets
        assertEquals(listOf(10 to 60.0, 8 to 80.0), bench.map { it.reps to it.weightKg })
        assertEquals(setOf(SetTag.Warmup), bench[0].tags)
        assertTrue(repeated.entries.flatMap { it.strengthSets }.all { it.performedAtUtc == null && it.rpe == null && !it.isPr })
        assertEquals(1, repeated.entries[1].supersetGroup)
    }

    @Test
    fun repeatingKeepsANameTheOwnerGave() {
        val past = WorkoutSession(name = "Push day", entries = listOf(WorkoutEntry(exerciseId = "bench")))

        assertEquals("Morning lift", WorkoutSession(name = "Morning lift").repeating(past).name)
    }

    @Test
    fun switchingToATemplateLinksItAndLoadsItsPlan() {
        val template = WorkoutTemplate(
            id = "t1",
            name = "Legs",
            exercises = listOf(WorkoutTemplateExercise(exerciseId = "squat", exerciseName = "Squat", targetSets = 3, repsMax = 5)),
        )

        val switched = WorkoutSession(id = "new").switchedTo(template)

        assertEquals("t1", switched.templateId)
        assertEquals("Legs", switched.name)
        assertEquals(listOf(5, 5, 5), switched.entries.single().strengthSets.map { it.reps })
        assertEquals("new", switched.entries.single().workoutSessionId)
    }
}
