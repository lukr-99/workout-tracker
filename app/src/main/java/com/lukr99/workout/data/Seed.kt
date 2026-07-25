package com.lukr99.workout.data

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource

/**
 * The 16 starter exercises, ported 1:1 from the MAUI `WorkoutTracker.Core/Seed/SeedExercises.cs`.
 * Inserted once, on an empty database, tagged `source = Seeded`. Ids are freshly generated (as in
 * MAUI) — seeded rows are not a cross-device identity, unlike imported ones.
 */
object Seed {

    fun exercises(): List<Exercise> = listOf(
        strength("Barbell Bench Press", "Chest", listOf("Shoulders", "Triceps"), "Barbell"),
        strength("Incline Dumbbell Press", "Chest", listOf("Shoulders", "Triceps"), "Dumbbells"),
        strength("Back Squat", "Legs", listOf("Glutes", "Abs"), "Barbell"),
        strength("Romanian Deadlift", "Legs", listOf("Back", "Glutes"), "Barbell"),
        strength("Lat Pulldown", "Back", listOf("Biceps"), "Cable"),
        strength("Seated Cable Row", "Back", listOf("Biceps"), "Cable"),
        strength("Overhead Press", "Shoulders", listOf("Triceps"), "Barbell"),
        strength("Lateral Raise", "Shoulders", emptyList(), "Dumbbells"),
        strength("Biceps Curl", "Arms", emptyList(), "Dumbbells"),
        strength("Triceps Pushdown", "Arms", emptyList(), "Cable"),
        strength("Plank", "Abs", emptyList(), "Bodyweight"),
        cardio("Treadmill Run", "Cardio", listOf("Legs"), "Treadmill"),
        cardio("Stationary Bike", "Cardio", listOf("Legs"), "Bike"),
        cardio("Rowing Machine", "Cardio", listOf("Back", "Legs"), "Rower"),
        cardio("Jump Rope", "Cardio", listOf("Calves"), "Rope"),
        cardio("Outdoor Walk", "Cardio", listOf("Legs"), "None"),
    )

    private fun strength(name: String, primary: String, secondary: List<String>, equipment: String) =
        Exercise(
            name = name,
            category = ExerciseCategory.Strength,
            primaryBodyPart = primary,
            secondaryBodyParts = secondary,
            equipment = equipment,
            source = ExerciseSource.Seeded,
        )

    private fun cardio(name: String, primary: String, secondary: List<String>, equipment: String) =
        Exercise(
            name = name,
            category = ExerciseCategory.Cardio,
            primaryBodyPart = primary,
            secondaryBodyParts = secondary,
            equipment = equipment,
            source = ExerciseSource.Seeded,
        )
}
