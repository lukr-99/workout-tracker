using System.Net.Http.Json;
using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.Core.Services;

public sealed class WgerSyncService : IExerciseCatalogSyncService
{
    private readonly HttpClient _httpClient;
    private readonly IWorkoutDataService _workoutDataService;

    public WgerSyncService(HttpClient httpClient, IWorkoutDataService workoutDataService)
    {
        _httpClient = httpClient;
        _workoutDataService = workoutDataService;
    }

    public async Task<int> SyncFromWgerAsync(int limit = 30, CancellationToken cancellationToken = default)
    {
        limit = Math.Clamp(limit, 1, 100);
        var pageSize = Math.Min(limit, 20);
        var offset = 0;
        var mapped = new List<Exercise>();

        while (mapped.Count < limit)
        {
            var response = await _httpClient.GetFromJsonAsync<WgerResponse>(
                $"https://wger.de/api/v2/exerciseinfo/?language=2&limit={pageSize}&offset={offset}",
                cancellationToken).ConfigureAwait(false);

            if (response?.Results is null || response.Results.Count == 0)
            {
                break;
            }

            foreach (var result in response.Results)
            {
                var translation = result.Translations?.FirstOrDefault(x => x.Language == 2 && !string.IsNullOrWhiteSpace(x.Name))
                    ?? result.Translations?.FirstOrDefault(x => !string.IsNullOrWhiteSpace(x.Name));

                mapped.Add(new Exercise
                {
                    Name = translation?.Name?.Trim() ?? "Imported Exercise",
                    Category = string.Equals(result.Category?.Name, "Cardio", StringComparison.OrdinalIgnoreCase)
                        ? ExerciseCategory.Cardio
                        : ExerciseCategory.Strength,
                    PrimaryBodyPart = result.Muscles?.FirstOrDefault()?.EnglishName
                        ?? result.Category?.Name
                        ?? "Full Body",
                    SecondaryBodyParts = result.MusclesSecondary?
                        .Select(x => x.EnglishName ?? x.Name)
                        .Where(x => !string.IsNullOrWhiteSpace(x))
                        .Select(x => x!)
                        .Distinct(StringComparer.OrdinalIgnoreCase)
                        .ToList() ?? [],
                    Equipment = string.Join(", ", result.Equipment?.Select(x => x.Name).Where(x => !string.IsNullOrWhiteSpace(x)) ?? []),
                    Notes = translation?.Description ?? string.Empty,
                    Source = ExerciseSource.Synced,
                    ExternalSourceId = result.Uuid ?? result.Id.ToString()
                });

                if (mapped.Count >= limit)
                {
                    break;
                }
            }

            offset += pageSize;
            if (string.IsNullOrWhiteSpace(response.Next))
            {
                break;
            }
        }

        return await _workoutDataService.MergeExternalExercisesAsync(mapped, cancellationToken).ConfigureAwait(false);
    }

    private sealed class WgerResponse
    {
        public string? Next { get; set; }
        public List<WgerExercise>? Results { get; set; }
    }

    private sealed class WgerExercise
    {
        public int Id { get; set; }
        public string? Uuid { get; set; }
        public WgerCategory? Category { get; set; }
        public List<WgerMuscle>? Muscles { get; set; }
        public List<WgerMuscle>? MusclesSecondary { get; set; }
        public List<WgerEquipment>? Equipment { get; set; }
        public List<WgerTranslation>? Translations { get; set; }
    }

    private sealed class WgerCategory
    {
        public string? Name { get; set; }
    }

    private sealed class WgerMuscle
    {
        public string? Name { get; set; }
        public string? Name_En { get; set; }
        public string? EnglishName => string.IsNullOrWhiteSpace(Name_En) ? Name : Name_En;
    }

    private sealed class WgerEquipment
    {
        public string? Name { get; set; }
    }

    private sealed class WgerTranslation
    {
        public int Language { get; set; }
        public string? Name { get; set; }
        public string? Description { get; set; }
    }
}
