package com.lukr99.workout.data.transfer

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImportPlannerTest {
    @Test
    fun exactDuplicate_isSkippedAndExerciseIdIsRemapped() {
        val existingExercise = Exercise(id = "catalog", name = "Bench Press", primaryBodyPart = "Chest")
        val incomingExercise = existingExercise.copy(id = "foreign")
        val existingSession = session("existing", "catalog")
        val incomingSession = session("foreign-session", "foreign")
        val preview = ImportPlanner.plan(
            ImportedPayload(
                DataFormat.WorkoutJson,
                exercises = listOf(incomingExercise),
                sessions = listOf(incomingSession),
            ),
            ImportContext(listOf(existingExercise), emptyList(), listOf(existingSession)),
            ImportOptions(),
        )

        assertEquals(PlannedAction.Skip, preview.plan.exercises.single().action)
        assertEquals("catalog", preview.plan.sessions.single().value.entries.single().exerciseId)
        assertEquals(PlannedAction.Skip, preview.plan.sessions.single().action)
    }

    @Test
    fun keepBoth_regeneratesEntireSessionGraph() {
        val existing = session("same", "bench")
        val preview = ImportPlanner.plan(
            ImportedPayload(DataFormat.WorkoutJson, sessions = listOf(existing)),
            ImportContext(emptyList(), emptyList(), listOf(existing)),
            ImportOptions(sessionConflictPolicy = ConflictPolicy.KeepBoth),
        )
        val planned = preview.plan.sessions.single()

        assertEquals(PlannedAction.KeepBoth, planned.action)
        assertNotEquals(existing.id, planned.value.id)
        assertNotEquals(existing.entries.single().id, planned.value.entries.single().id)
        assertNotEquals(
            existing.entries.single().strengthSets.single().id,
            planned.value.entries.single().strengthSets.single().id,
        )
        assertEquals(planned.value.id, planned.value.entries.single().workoutSessionId)
    }

    @Test
    fun runsAndRoutes_insertOnlyTheOnesNotAlreadyPresent() {
        val preview = ImportPlanner.plan(
            ImportedPayload(
                DataFormat.WorkoutJson,
                runs = listOf(
                    Run(id = "r-existing", distanceMeters = 1000.0),
                    Run(
                        id = "r-new",
                        distanceMeters = 2000.0,
                        trace = listOf(TracePoint(0, 50.0, 14.0), TracePoint(1000, 50.01, 14.0)),
                    ),
                ),
                routes = listOf(
                    Route(id = "route-existing", name = "Home loop"),
                    Route(id = "route-new", name = "River"),
                ),
            ),
            ImportContext(
                exercises = emptyList(),
                templates = emptyList(),
                sessions = emptyList(),
                existingRunIds = setOf("r-existing"),
                existingRouteIds = setOf("route-existing"),
            ),
            ImportOptions(),
        )

        assertEquals(listOf("r-new"), preview.plan.runs.map { it.id })
        assertEquals(listOf("route-new"), preview.plan.routes.map { it.id })
        assertEquals(1, preview.summary.insertedRuns)
        assertEquals(1, preview.summary.insertedRoutes)
        // The restored run keeps its full trace so the map redraws after a restore.
        assertEquals(2, preview.plan.runs.single().trace.size)
    }

    private fun session(id: String, exerciseId: String): WorkoutSession {
        val entryId = "$id-entry"
        return WorkoutSession(
            id = id,
            name = "Push",
            startedAtUtc = 1_700_000_000_000L,
            durationSeconds = 3_600,
            status = WorkoutSessionStatus.Completed,
            entries = listOf(
                WorkoutEntry(
                    id = entryId,
                    workoutSessionId = id,
                    exerciseId = exerciseId,
                    exerciseSnapshotName = "Bench Press",
                    exerciseSnapshotCategory = ExerciseCategory.Strength,
                    entryType = ExerciseCategory.Strength,
                    strengthSets = listOf(
                        StrengthSet(
                            id = "$entryId-set",
                            workoutEntryId = entryId,
                            reps = 5,
                            weightKg = 100.0,
                        ),
                    ),
                ),
            ),
        )
    }
}
