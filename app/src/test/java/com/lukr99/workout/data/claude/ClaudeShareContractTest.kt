package com.lukr99.workout.data.claude

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The share is a contract with the Worker (cloud/src/workouts/SharedBundle.ts). This writes a fixed
 * share the way the phone sends it and compares it with cloud/test/fixtures/ember-share.json, which
 * the Worker's tests read too, so a change on either side fails a test. After a deliberate change,
 * set EMBER_RECORD_SHARE=1 to write the file again, and update the Worker with it.
 */
class ClaudeShareContractTest {
    @Test
    fun theShareMatchesTheFileTheWorkerReads() {
        val json = ClaudeShareBuilder.json.encodeToString(ClaudeShare.serializer(), sample()) + "\n"
        val file = File("../cloud/test/fixtures/ember-share.json")
        if (System.getenv("EMBER_RECORD_SHARE") == "1") {
            file.parentFile.mkdirs()
            file.writeText(json)
        }
        assertEquals(file.readText().replace("\r\n", "\n"), json)
    }

    private fun sample(): ClaudeShare {
        val start = 1_762_779_600_000L // 2025-11-10T13:00:00Z
        val bench = Exercise(id = "ex-bench", name = "Bench Press", primaryBodyPart = "Chest", secondaryBodyParts = listOf("Triceps"), equipment = "Barbell", instructions = "Lie down\nPress", defaultRestSeconds = 180)
        val run = Exercise(id = "ex-run", name = "Treadmill", category = ExerciseCategory.Cardio, primaryBodyPart = "Cardio")
        val gym = Location(id = "loc-gym", name = "Iron Gym")
        val session = WorkoutSession(
            id = "w-1",
            templateId = "t-push",
            name = "Push A",
            startedAtUtc = start,
            endedAtUtc = start + 3_600_000,
            completedDateUtc = start + 3_600_000,
            durationSeconds = 3600,
            status = WorkoutSessionStatus.Completed,
            perceivedEffort = 8,
            locationId = gym.id,
            entries = listOf(
                WorkoutEntry(
                    id = "e-1",
                    workoutSessionId = "w-1",
                    exerciseId = bench.id,
                    exerciseSnapshotName = bench.name,
                    exerciseSnapshotPrimaryBodyPart = "Chest",
                    supersetGroup = 1,
                    strengthSets = listOf(
                        StrengthSet(id = "s-1", workoutEntryId = "e-1", setNumber = 1, reps = 10, weightKg = 40.0, isWarmup = true, tags = setOf(SetTag.Warmup)),
                        StrengthSet(id = "s-2", workoutEntryId = "e-1", setNumber = 2, reps = 5, weightKg = 85.0, rir = 2.0),
                        StrengthSet(id = "s-3", workoutEntryId = "e-1", setNumber = 3, reps = 4, weightKg = 85.0, rpe = 9.5, isPr = true, tags = setOf(SetTag.ToFailure, SetTag.Finisher)),
                    ),
                ),
            ),
        )
        val template = WorkoutTemplate(
            id = "t-push",
            name = "Push A",
            exercises = listOf(WorkoutTemplateExercise(id = "te-1", exerciseId = bench.id, exerciseName = bench.name, bodyPart = "Chest", notes = "RIR 2", targetSets = 3, repsMin = 5, repsMax = 8, restSeconds = 180)),
        )
        val trace = (0..23).map { i -> TracePoint(t = start + 86_400_000 + i * 33_000L, lat = 50.0 + i * 0.0009, lon = 14.4) }
        return ClaudeShare(
            timeZone = "Europe/Prague",
            units = "metric",
            appVersion = "2.11.0",
            bundle = ExportBundle(
                exportedAtUtc = "2025-11-11T08:00:00Z",
                exercises = listOf(bench, run),
                templates = listOf(template),
                sessions = listOf(session),
                appVersion = "2.11.0",
                locations = Location.builtIns + gym,
            ),
            runs = listOf(
                SharedRun.of(
                    Run(id = "r-1", startedAtUtc = start + 86_400_000, durationSeconds = 760, movingSeconds = 759, distanceMeters = 2301.0, avgPaceSecPerKm = 330.3, elevationGainM = 6.0, avgHr = 150, notes = "Easy\nlegs fine", trace = trace),
                ),
            ),
        )
    }
}
