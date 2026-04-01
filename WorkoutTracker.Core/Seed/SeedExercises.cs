using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.Core.Seed;

public static class SeedExercises
{
    public static IReadOnlyList<Exercise> Create() =>
    [
        NewStrength("Barbell Bench Press", "Chest", ["Shoulders", "Triceps"], "Barbell"),
        NewStrength("Incline Dumbbell Press", "Chest", ["Shoulders", "Triceps"], "Dumbbells"),
        NewStrength("Back Squat", "Legs", ["Glutes", "Abs"], "Barbell"),
        NewStrength("Romanian Deadlift", "Legs", ["Back", "Glutes"], "Barbell"),
        NewStrength("Lat Pulldown", "Back", ["Biceps"], "Cable"),
        NewStrength("Seated Cable Row", "Back", ["Biceps"], "Cable"),
        NewStrength("Overhead Press", "Shoulders", ["Triceps"], "Barbell"),
        NewStrength("Lateral Raise", "Shoulders", [], "Dumbbells"),
        NewStrength("Biceps Curl", "Arms", [], "Dumbbells"),
        NewStrength("Triceps Pushdown", "Arms", [], "Cable"),
        NewStrength("Plank", "Abs", [], "Bodyweight"),
        NewCardio("Treadmill Run", "Cardio", ["Legs"], "Treadmill"),
        NewCardio("Stationary Bike", "Cardio", ["Legs"], "Bike"),
        NewCardio("Rowing Machine", "Cardio", ["Back", "Legs"], "Rower"),
        NewCardio("Jump Rope", "Cardio", ["Calves"], "Rope"),
        NewCardio("Outdoor Walk", "Cardio", ["Legs"], "None")
    ];

    private static Exercise NewStrength(string name, string primary, IEnumerable<string> secondary, string equipment) =>
        new()
        {
            Name = name,
            Category = ExerciseCategory.Strength,
            PrimaryBodyPart = primary,
            SecondaryBodyParts = secondary.ToList(),
            Equipment = equipment,
            Source = ExerciseSource.Seeded
        };

    private static Exercise NewCardio(string name, string primary, IEnumerable<string> secondary, string equipment) =>
        new()
        {
            Name = name,
            Category = ExerciseCategory.Cardio,
            PrimaryBodyPart = primary,
            SecondaryBodyParts = secondary.ToList(),
            Equipment = equipment,
            Source = ExerciseSource.Seeded
        };
}
