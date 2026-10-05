package com.lukr99.workout.domain.creation

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.WorkoutSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutFactoryTest {
    private var nextId = 0
    private val factory = WorkoutFactory(
        ids = IdGenerator { "id${++nextId}" },
        clock = TimeProvider { 1_000_000L },
    )

    @Test
    fun session_normalizesGraphAndSnapshotsCatalog() {
        val exercise = Exercise(
            id = "bench",
            name = "Barbell Bench Press",
            category = ExerciseCategory.Strength,
            primaryBodyPart = "Chest",
        )
        val result = factory.session(
            SessionDraft(
                name = "  Push  ",
                status = WorkoutSessionStatus.Completed,
                durationSeconds = 3_600,
                entries = listOf(
                    EntryDraft(
                        exerciseId = exercise.id,
                        strengthSets = listOf(
                            StrengthSetDraft(reps = 5, weightKg = 100.0, setType = SetType.Warmup),
                            StrengthSetDraft(reps = 3, weightKg = 110.0),
                        ),
                    ),
                ),
            ),
            catalog = mapOf(exercise.id to exercise),
        )

        assertTrue(result.isValid)
        val session = result.value
        assertEquals("Push", session.name)
        assertEquals(1_000_000L, session.startedAtUtc)
        assertEquals(4_600_000L, session.endedAtUtc)
        val entry = session.entries.single()
        assertEquals("Barbell Bench Press", entry.exerciseSnapshotName)
        assertEquals("Chest", entry.exerciseSnapshotPrimaryBodyPart)
        assertEquals(listOf(1, 2), entry.strengthSets.map { it.setNumber })
        assertTrue(entry.strengthSets.first().isWarmup)
        assertEquals(entry.id, entry.strengthSets.first().workoutEntryId)
    }

    @Test
    fun invalidDraft_returnsIssuesWithoutThrowing() {
        val result = factory.session(
            SessionDraft(
                perceivedEffort = 42,
                bodyweightKg = -1.0,
                entries = listOf(
                    EntryDraft(strengthSets = listOf(StrengthSetDraft(reps = -2, weightKg = -10.0))),
                ),
            ),
            policy = CreationPolicy(requireCatalogExercise = true),
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.path == "session.perceivedEffort" })
        assertTrue(result.issues.any { it.path.endsWith("weightKg") })
        assertEquals(0.0, result.value.entries.single().strengthSets.single().weightKg, 0.0)
    }

    @Test
    fun template_keepsThePlanAndOrdersTheRepRange() {
        val result = factory.template(
            TemplateDraft(
                name = "Push",
                exercises = listOf(
                    TemplateExerciseDraft(exerciseName = "Bench", targetSets = 4, repsMin = 8, repsMax = 6, restSeconds = 150, supersetGroup = 1),
                    TemplateExerciseDraft(exerciseName = "Dips", targetSets = 99, restSeconds = -5),
                ),
            ),
        )

        val (bench, dips) = result.value.exercises
        assertEquals(4, bench.targetSets)
        assertEquals(6, bench.repsMin)
        assertEquals(8, bench.repsMax)
        assertEquals(150, bench.restSeconds)
        assertEquals(1, bench.supersetGroup)
        assertEquals(20, dips.targetSets)
        assertEquals(0, dips.restSeconds)
        assertEquals(null, dips.repsMin)
    }
}
