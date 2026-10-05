package com.lukr99.workout.ui.screens

import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.domain.toNewEntry

/** A small training history, relative to now so Home's "this week" and "recent" are filled. */
internal object ScreenFixtures {
    private const val DAY = 24L * 60 * 60 * 1000

    suspend fun fill(repo: WorkoutRepository) {
        val catalog = repo.getExercises().associateBy(Exercise::name)
        fun ex(name: String) = checkNotNull(catalog[name]) { "Seed has no $name" }
        val bench = ex("Barbell Bench Press").let {
            repo.saveExercise(it.copy(notes = "Seat 4. Grip one finger outside the rings."))
        }
        val incline = ex("Incline Dumbbell Press")
        val squat = ex("Back Squat")

        repo.saveTemplate(
            WorkoutTemplate(
                name = "Push day",
                notes = "Heavy bench first.",
                exercises = listOf(bench, incline).mapIndexed { i, e ->
                    WorkoutTemplateExercise(
                        exerciseId = e.id, exerciseName = e.name, category = e.category, bodyPart = e.primaryBodyPart, sortOrder = i,
                        targetSets = if (i == 0) 4 else 3, repsMin = if (i == 0) 6 else 8, repsMax = if (i == 0) 8 else 10,
                        restSeconds = if (i == 0) 150 else 90, notes = if (i == 0) "Pause on the chest" else "",
                    )
                },
            ),
        )
        repo.saveTemplate(
            WorkoutTemplate(
                name = "Legs",
                exercises = listOf(
                    WorkoutTemplateExercise(exerciseId = squat.id, exerciseName = squat.name, category = squat.category, bodyPart = squat.primaryBodyPart),
                ),
            ),
        )

        val now = System.currentTimeMillis()
        listOf(9 to 77.5, 5 to 80.0, 2 to 82.5).forEach { (daysAgo, kg) ->
            val start = now - daysAgo * DAY
            repo.saveWorkoutSession(
                WorkoutSession(
                    name = "Push day",
                    status = WorkoutSessionStatus.Completed,
                    startedAtUtc = start,
                    endedAtUtc = start + 55 * 60_000,
                    entries = listOf(
                        bench.toNewEntry(0).copy(strengthSets = sets(kg, 8, 8, 7, start)),
                        incline.toNewEntry(1).copy(strengthSets = sets(30.0, 10, 10, 9, start)),
                    ),
                ),
            )
        }
        repo.saveWorkoutSession(
            WorkoutSession(
                name = "Push day",
                status = WorkoutSessionStatus.Active,
                startedAtUtc = now - 24 * 60_000,
                notes = "Short on time, skip dips if needed.",
                entries = listOf(
                    bench.toNewEntry(0).copy(
                        notes = "Felt strong",
                        startedAtUtc = now - 20 * 60_000,
                        strengthSets = sets(82.5, 8, 8, 0, now - 20 * 60_000),
                    ),
                    incline.toNewEntry(1),
                ),
            ),
        )
    }

    /** Three runs and a saved route around a park, so the Runs tab has shapes to draw. */
    suspend fun fillRuns(runs: RunRepository) {
        if (runs.getRuns().isNotEmpty()) return
        val now = System.currentTimeMillis()
        val loop = listOf(50.080 to 14.420, 50.084 to 14.426, 50.083 to 14.436, 50.077 to 14.438, 50.074 to 14.430, 50.076 to 14.421, 50.080 to 14.420)
        val outAndBack = listOf(50.070 to 14.400, 50.075 to 14.410, 50.081 to 14.413, 50.086 to 14.425, 50.081 to 14.413, 50.070 to 14.400)
        listOf(Triple(1L, loop, 7_220.0), Triple(5L, outAndBack, 5_080.0), Triple(8L, loop, 4_100.0)).forEach { (daysAgo, path, meters) ->
            runs.saveRun(
                Run(
                    startedAtUtc = now - daysAgo * DAY,
                    durationSeconds = (meters / 1000 * 330).toLong(),
                    movingSeconds = (meters / 1000 * 325).toLong(),
                    distanceMeters = meters,
                    avgPaceSecPerKm = 325.0,
                    encodedPolyline = Polyline.encode(path),
                    notes = if (daysAgo == 1L) "Morning run" else "",
                ),
            )
        }
        runs.saveRoute(Route(name = "River loop", distanceMeters = 5_100.0, encodedPolyline = Polyline.encode(loop)))
    }

    private fun sets(kg: Double, a: Int, b: Int, c: Int, at: Long) = listOf(a, b, c).mapIndexed { i, reps ->
        StrengthSet(setNumber = i + 1, reps = reps, weightKg = kg, performedAtUtc = if (reps > 0) at + i * 180_000 else null)
    }
}
