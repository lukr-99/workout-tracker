package com.lukr99.workout.ui.screens

import com.lukr99.workout.data.WorkoutRepository
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
                    WorkoutTemplateExercise(exerciseId = e.id, exerciseName = e.name, category = e.category, bodyPart = e.primaryBodyPart, sortOrder = i)
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

    private fun sets(kg: Double, a: Int, b: Int, c: Int, at: Long) = listOf(a, b, c).mapIndexed { i, reps ->
        StrengthSet(setNumber = i + 1, reps = reps, weightKg = kg, performedAtUtc = if (reps > 0) at + i * 180_000 else null)
    }
}
