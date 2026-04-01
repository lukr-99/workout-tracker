using System.Globalization;

namespace WorkoutTracker.Core.Domain;

public enum ExerciseCategory
{
    Strength = 0,
    Cardio = 1
}

public enum ExerciseSource
{
    Seeded = 0,
    Synced = 1,
    Custom = 2
}

public enum WorkoutSessionStatus
{
    Active = 0,
    Completed = 1,
    Discarded = 2
}

public sealed class Exercise
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string Name { get; set; } = string.Empty;
    public ExerciseCategory Category { get; set; } = ExerciseCategory.Strength;
    public string PrimaryBodyPart { get; set; } = string.Empty;
    public List<string> SecondaryBodyParts { get; set; } = [];
    public string Equipment { get; set; } = string.Empty;
    public string Notes { get; set; } = string.Empty;
    public ExerciseSource Source { get; set; } = ExerciseSource.Custom;
    public string? ExternalSourceId { get; set; }
    public bool IsArchived { get; set; }
    public string BodyPartsSummary =>
        string.Join(" / ",
            new[] { PrimaryBodyPart }
                .Concat(SecondaryBodyParts)
                .Where(x => !string.IsNullOrWhiteSpace(x))
                .Distinct(StringComparer.OrdinalIgnoreCase));
}

public sealed class ExerciseFilter
{
    public string SearchText { get; set; } = string.Empty;
    public string BodyPart { get; set; } = string.Empty;
    public ExerciseCategory? Category { get; set; }
    public bool IncludeArchived { get; set; }
}

public sealed class WorkoutTemplate
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string Name { get; set; } = string.Empty;
    public string Notes { get; set; } = string.Empty;
    public List<WorkoutTemplateExercise> Exercises { get; set; } = [];
}

public sealed class WorkoutTemplateExercise
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string ExerciseId { get; set; } = string.Empty;
    public string ExerciseName { get; set; } = string.Empty;
    public ExerciseCategory Category { get; set; } = ExerciseCategory.Strength;
    public string BodyPart { get; set; } = string.Empty;
    public int SortOrder { get; set; }
    public string Notes { get; set; } = string.Empty;
}

public sealed class WorkoutSession
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string? TemplateId { get; set; }
    public string Name { get; set; } = string.Empty;
    public DateTime StartedAtUtc { get; set; } = DateTime.UtcNow;
    public DateTime? EndedAtUtc { get; set; }
    public DateTime? CompletedDateUtc { get; set; }
    public long DurationSeconds { get; set; }
    public string Notes { get; set; } = string.Empty;
    public WorkoutSessionStatus Status { get; set; } = WorkoutSessionStatus.Active;
    public List<WorkoutEntry> Entries { get; set; } = [];
}

public sealed class WorkoutEntry
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string WorkoutSessionId { get; set; } = string.Empty;
    public string ExerciseId { get; set; } = string.Empty;
    public string ExerciseSnapshotName { get; set; } = string.Empty;
    public ExerciseCategory ExerciseSnapshotCategory { get; set; } = ExerciseCategory.Strength;
    public string ExerciseSnapshotPrimaryBodyPart { get; set; } = string.Empty;
    public int SortOrder { get; set; }
    public ExerciseCategory EntryType { get; set; } = ExerciseCategory.Strength;
    public string Notes { get; set; } = string.Empty;
    public List<StrengthSet> StrengthSets { get; set; } = [];
    public CardioEntryData? CardioData { get; set; }
    public bool IsStrength => EntryType == ExerciseCategory.Strength;
}

public sealed class StrengthSet
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string WorkoutEntryId { get; set; } = string.Empty;
    public int SetNumber { get; set; }
    public int Reps { get; set; }
    public decimal WeightKg { get; set; }
    public decimal? Rir { get; set; }
    public decimal? Rpe { get; set; }
    public DateTime? PerformedAtUtc { get; set; }
    public string Notes { get; set; } = string.Empty;
}

public sealed class CardioEntryData
{
    public string WorkoutEntryId { get; set; } = string.Empty;
    public int DurationSeconds { get; set; }
    public decimal? DistanceKm { get; set; }
    public decimal? Calories { get; set; }
    public string Notes { get; set; } = string.Empty;
}

public sealed class WorkoutSessionSummary
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public DateTime StartedAtUtc { get; set; }
    public DateTime? CompletedDateUtc { get; set; }
    public WorkoutSessionStatus Status { get; set; }
    public int ExerciseCount { get; set; }
    public int StrengthSetCount { get; set; }
    public decimal TotalVolumeKg { get; set; }
    public int CardioMinutes { get; set; }
    public string SessionTypeLabel { get; set; } = string.Empty;
    public string BodyPartsSummary { get; set; } = string.Empty;
}

public sealed class AnalyticsOverview
{
    public int TotalCompletedWorkouts { get; set; }
    public int WorkoutsLast30Days { get; set; }
    public decimal TotalVolumeKg { get; set; }
    public int CurrentWeeklyStreak { get; set; }
    public string MostLoggedExerciseName { get; set; } = string.Empty;
}

public sealed class WorkoutConsistencySnapshot
{
    public int WorkoutsLast7Days { get; set; }
    public int WorkoutsLast30Days { get; set; }
    public int CurrentWeeklyStreak { get; set; }
    public int LongestGapDays { get; set; }
}

public sealed class ExerciseAnalyticsPoint
{
    public string ExerciseId { get; set; } = string.Empty;
    public DateTime DateUtc { get; set; }
    public decimal BestWeightKg { get; set; }
    public decimal TotalVolumeKg { get; set; }
    public int TotalReps { get; set; }
    public string SessionName { get; set; } = string.Empty;
}

public sealed class DashboardSnapshot
{
    public WorkoutSession? ActiveWorkout { get; set; }
    public AnalyticsOverview Analytics { get; set; } = new();
    public WorkoutConsistencySnapshot Consistency { get; set; } = new();
    public IReadOnlyList<WorkoutSessionSummary> RecentWorkouts { get; set; } = Array.Empty<WorkoutSessionSummary>();
}

public sealed class ExportBundle
{
    public string ExportedAtUtc { get; set; } = DateTime.UtcNow.ToString("O", CultureInfo.InvariantCulture);
    public string ExportFormatVersion { get; set; } = "1.0";
    public List<Exercise> Exercises { get; set; } = [];
    public List<WorkoutTemplate> Templates { get; set; } = [];
    public List<WorkoutSession> Sessions { get; set; } = [];
}

public sealed class ExportedFiles
{
    public string JsonPath { get; set; } = string.Empty;
    public IReadOnlyList<string> CsvPaths { get; set; } = Array.Empty<string>();
}
