using SQLite;
using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.Core.Data;

[Table("Exercises")]
public sealed class ExerciseRecord
{
    [PrimaryKey]
    public string Id { get; set; } = string.Empty;

    [Indexed]
    public string Name { get; set; } = string.Empty;

    public ExerciseCategory Category { get; set; }
    public string PrimaryBodyPart { get; set; } = string.Empty;
    public string SecondaryBodyPartsJson { get; set; } = "[]";
    public string Equipment { get; set; } = string.Empty;
    public string Notes { get; set; } = string.Empty;
    public ExerciseSource Source { get; set; }

    [Indexed]
    public string? ExternalSourceId { get; set; }

    public bool IsArchived { get; set; }
}

[Table("WorkoutTemplates")]
public sealed class WorkoutTemplateRecord
{
    [PrimaryKey]
    public string Id { get; set; } = string.Empty;

    [Indexed]
    public string Name { get; set; } = string.Empty;

    public string Notes { get; set; } = string.Empty;
}

[Table("WorkoutTemplateExercises")]
public sealed class WorkoutTemplateExerciseRecord
{
    [PrimaryKey]
    public string Id { get; set; } = string.Empty;

    [Indexed]
    public string TemplateId { get; set; } = string.Empty;

    public string ExerciseId { get; set; } = string.Empty;
    public string ExerciseName { get; set; } = string.Empty;
    public ExerciseCategory Category { get; set; }
    public string BodyPart { get; set; } = string.Empty;
    public int SortOrder { get; set; }
    public string Notes { get; set; } = string.Empty;
}

[Table("WorkoutSessions")]
public sealed class WorkoutSessionRecord
{
    [PrimaryKey]
    public string Id { get; set; } = string.Empty;

    [Indexed]
    public string? TemplateId { get; set; }

    [Indexed]
    public string Name { get; set; } = string.Empty;

    [Indexed]
    public WorkoutSessionStatus Status { get; set; }

    public DateTime StartedAtUtc { get; set; }
    public DateTime? EndedAtUtc { get; set; }
    public DateTime? CompletedDateUtc { get; set; }
    public long DurationSeconds { get; set; }
    public string Notes { get; set; } = string.Empty;
}

[Table("WorkoutEntries")]
public sealed class WorkoutEntryRecord
{
    [PrimaryKey]
    public string Id { get; set; } = string.Empty;

    [Indexed]
    public string WorkoutSessionId { get; set; } = string.Empty;

    public string ExerciseId { get; set; } = string.Empty;
    public string ExerciseSnapshotName { get; set; } = string.Empty;
    public ExerciseCategory ExerciseSnapshotCategory { get; set; }
    public string ExerciseSnapshotPrimaryBodyPart { get; set; } = string.Empty;
    public int SortOrder { get; set; }
    public ExerciseCategory EntryType { get; set; }
    public string Notes { get; set; } = string.Empty;
}

[Table("StrengthSets")]
public sealed class StrengthSetRecord
{
    [PrimaryKey]
    public string Id { get; set; } = string.Empty;

    [Indexed]
    public string WorkoutEntryId { get; set; } = string.Empty;

    public int SetNumber { get; set; }
    public int Reps { get; set; }
    public decimal WeightKg { get; set; }
    public decimal? Rir { get; set; }
    public decimal? Rpe { get; set; }
    public DateTime? PerformedAtUtc { get; set; }
    public string Notes { get; set; } = string.Empty;
}

[Table("CardioEntries")]
public sealed class CardioEntryRecord
{
    [PrimaryKey]
    public string WorkoutEntryId { get; set; } = string.Empty;

    public int DurationSeconds { get; set; }
    public decimal? DistanceKm { get; set; }
    public decimal? Calories { get; set; }
    public string Notes { get; set; } = string.Empty;
}
