using System.Text;
using System.Text.Json;
using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.Core.Services;

public sealed class ExportService : IExportService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web) { WriteIndented = true };
    private readonly IWorkoutDataService _workoutDataService;

    public ExportService(IWorkoutDataService workoutDataService)
    {
        _workoutDataService = workoutDataService;
    }

    public async Task<ExportedFiles> ExportAsync(string outputDirectory, CancellationToken cancellationToken = default)
    {
        Directory.CreateDirectory(outputDirectory);
        var bundle = await _workoutDataService.CreateExportBundleAsync(cancellationToken).ConfigureAwait(false);
        var timestamp = DateTime.UtcNow.ToString("yyyyMMdd-HHmmss");
        var jsonPath = Path.Combine(outputDirectory, $"workout-export-{timestamp}.json");
        await File.WriteAllTextAsync(jsonPath, JsonSerializer.Serialize(bundle, JsonOptions), cancellationToken).ConfigureAwait(false);

        var csvPaths = new List<string>
        {
            await WriteExercisesCsvAsync(outputDirectory, timestamp, bundle.Exercises, cancellationToken).ConfigureAwait(false),
            await WriteTemplatesCsvAsync(outputDirectory, timestamp, bundle.Templates, cancellationToken).ConfigureAwait(false),
            await WriteSessionsCsvAsync(outputDirectory, timestamp, bundle.Sessions, cancellationToken).ConfigureAwait(false),
            await WriteEntriesCsvAsync(outputDirectory, timestamp, bundle.Sessions, cancellationToken).ConfigureAwait(false),
            await WriteStrengthSetsCsvAsync(outputDirectory, timestamp, bundle.Sessions, cancellationToken).ConfigureAwait(false),
            await WriteCardioCsvAsync(outputDirectory, timestamp, bundle.Sessions, cancellationToken).ConfigureAwait(false)
        };

        return new ExportedFiles
        {
            JsonPath = jsonPath,
            CsvPaths = csvPaths
        };
    }

    private static Task<string> WriteExercisesCsvAsync(string directory, string timestamp, IReadOnlyList<Exercise> exercises, CancellationToken cancellationToken) =>
        WriteCsvAsync(Path.Combine(directory, $"exercises-{timestamp}.csv"),
            ["Id", "Name", "Category", "PrimaryBodyPart", "SecondaryBodyParts", "Equipment", "Source", "ExternalSourceId", "Archived"],
            exercises.Select(x => new[]
            {
                x.Id, x.Name, x.Category.ToString(), x.PrimaryBodyPart, string.Join("|", x.SecondaryBodyParts), x.Equipment, x.Source.ToString(), x.ExternalSourceId ?? string.Empty, x.IsArchived.ToString()
            }),
            cancellationToken);

    private static Task<string> WriteTemplatesCsvAsync(string directory, string timestamp, IReadOnlyList<WorkoutTemplate> templates, CancellationToken cancellationToken) =>
        WriteCsvAsync(Path.Combine(directory, $"templates-{timestamp}.csv"),
            ["TemplateId", "TemplateName", "Notes", "ExerciseId", "ExerciseName", "Category", "BodyPart", "SortOrder"],
            templates.SelectMany(template => template.Exercises.Select(exercise => new[]
            {
                template.Id, template.Name, template.Notes, exercise.ExerciseId, exercise.ExerciseName, exercise.Category.ToString(), exercise.BodyPart, exercise.SortOrder.ToString()
            })),
            cancellationToken);

    private static Task<string> WriteSessionsCsvAsync(string directory, string timestamp, IReadOnlyList<WorkoutSession> sessions, CancellationToken cancellationToken) =>
        WriteCsvAsync(Path.Combine(directory, $"sessions-{timestamp}.csv"),
            ["SessionId", "Name", "TemplateId", "Status", "StartedAtUtc", "EndedAtUtc", "CompletedDateUtc", "DurationSeconds", "Notes"],
            sessions.Select(session => new[]
            {
                session.Id, session.Name, session.TemplateId ?? string.Empty, session.Status.ToString(), session.StartedAtUtc.ToString("O"), session.EndedAtUtc?.ToString("O") ?? string.Empty, session.CompletedDateUtc?.ToString("O") ?? string.Empty, session.DurationSeconds.ToString(), session.Notes
            }),
            cancellationToken);

    private static Task<string> WriteEntriesCsvAsync(string directory, string timestamp, IReadOnlyList<WorkoutSession> sessions, CancellationToken cancellationToken) =>
        WriteCsvAsync(Path.Combine(directory, $"entries-{timestamp}.csv"),
            ["EntryId", "SessionId", "ExerciseId", "ExerciseSnapshotName", "BodyPart", "EntryType", "SortOrder", "Notes"],
            sessions.SelectMany(session => session.Entries.Select(entry => new[]
            {
                entry.Id, session.Id, entry.ExerciseId, entry.ExerciseSnapshotName, entry.ExerciseSnapshotPrimaryBodyPart, entry.EntryType.ToString(), entry.SortOrder.ToString(), entry.Notes
            })),
            cancellationToken);

    private static Task<string> WriteStrengthSetsCsvAsync(string directory, string timestamp, IReadOnlyList<WorkoutSession> sessions, CancellationToken cancellationToken) =>
        WriteCsvAsync(Path.Combine(directory, $"strength_sets-{timestamp}.csv"),
            ["SetId", "EntryId", "SetNumber", "Reps", "WeightKg", "Rir", "Rpe", "PerformedAtUtc", "Notes"],
            sessions.SelectMany(session => session.Entries)
                .SelectMany(entry => entry.StrengthSets.Select(set => new[]
                {
                    set.Id, entry.Id, set.SetNumber.ToString(), set.Reps.ToString(), set.WeightKg.ToString(), set.Rir?.ToString() ?? string.Empty, set.Rpe?.ToString() ?? string.Empty, set.PerformedAtUtc?.ToString("O") ?? string.Empty, set.Notes
                })),
            cancellationToken);

    private static Task<string> WriteCardioCsvAsync(string directory, string timestamp, IReadOnlyList<WorkoutSession> sessions, CancellationToken cancellationToken) =>
        WriteCsvAsync(Path.Combine(directory, $"cardio_entries-{timestamp}.csv"),
            ["EntryId", "DurationSeconds", "DistanceKm", "Calories", "Notes"],
            sessions.SelectMany(session => session.Entries)
                .Where(entry => entry.CardioData is not null)
                .Select(entry => new[]
                {
                    entry.Id, entry.CardioData!.DurationSeconds.ToString(), entry.CardioData.DistanceKm?.ToString() ?? string.Empty, entry.CardioData.Calories?.ToString() ?? string.Empty, entry.CardioData.Notes
                }),
            cancellationToken);

    private static async Task<string> WriteCsvAsync(string path, IReadOnlyList<string> headers, IEnumerable<string[]> rows, CancellationToken cancellationToken)
    {
        var builder = new StringBuilder();
        builder.AppendLine(string.Join(",", headers.Select(Escape)));
        foreach (var row in rows)
        {
            builder.AppendLine(string.Join(",", row.Select(Escape)));
        }

        await File.WriteAllTextAsync(path, builder.ToString(), cancellationToken).ConfigureAwait(false);
        return path;
    }

    private static string Escape(string value)
    {
        return $"\"{value.Replace("\"", "\"\"")}\"";
    }
}
