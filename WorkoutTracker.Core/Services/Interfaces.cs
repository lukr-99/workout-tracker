using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.Core.Services;

public interface IWorkoutDataService
{
    Task InitializeAsync(CancellationToken cancellationToken = default);
    Task<DashboardSnapshot> GetDashboardSnapshotAsync(CancellationToken cancellationToken = default);
    Task<IReadOnlyList<Exercise>> GetExercisesAsync(ExerciseFilter? filter = null, CancellationToken cancellationToken = default);
    Task<Exercise?> GetExerciseAsync(string id, CancellationToken cancellationToken = default);
    Task<Exercise> SaveExerciseAsync(Exercise exercise, CancellationToken cancellationToken = default);
    Task ArchiveExerciseAsync(string id, CancellationToken cancellationToken = default);
    Task<int> MergeExternalExercisesAsync(IEnumerable<Exercise> exercises, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<WorkoutTemplate>> GetTemplatesAsync(CancellationToken cancellationToken = default);
    Task<WorkoutTemplate?> GetTemplateAsync(string id, CancellationToken cancellationToken = default);
    Task<WorkoutTemplate> SaveTemplateAsync(WorkoutTemplate template, CancellationToken cancellationToken = default);
    Task DeleteTemplateAsync(string id, CancellationToken cancellationToken = default);
    Task<WorkoutSession> CreateWorkoutSessionAsync(string? templateId = null, string? name = null, CancellationToken cancellationToken = default);
    Task<WorkoutSession?> GetActiveWorkoutAsync(CancellationToken cancellationToken = default);
    Task<WorkoutSession?> GetWorkoutSessionAsync(string id, CancellationToken cancellationToken = default);
    Task<WorkoutSession> SaveWorkoutSessionAsync(WorkoutSession session, CancellationToken cancellationToken = default);
    Task DeleteWorkoutSessionAsync(string id, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<WorkoutSessionSummary>> GetWorkoutHistoryAsync(string? searchText = null, CancellationToken cancellationToken = default);
    Task<WorkoutTemplate> DuplicateWorkoutAsTemplateAsync(string sessionId, string? templateName = null, CancellationToken cancellationToken = default);
    Task<ExportBundle> CreateExportBundleAsync(CancellationToken cancellationToken = default);
}

public interface IWorkoutHistoryService
{
    Task<IReadOnlyList<WorkoutSessionSummary>> GetWorkoutHistoryAsync(string? searchText = null, CancellationToken cancellationToken = default);
}

public interface IAnalyticsService
{
    Task<AnalyticsOverview> GetAnalyticsOverviewAsync(CancellationToken cancellationToken = default);
    Task<WorkoutConsistencySnapshot> GetConsistencySnapshotAsync(CancellationToken cancellationToken = default);
    Task<IReadOnlyList<ExerciseAnalyticsPoint>> GetExerciseProgressAsync(string exerciseId, CancellationToken cancellationToken = default);
}

public interface IExerciseCatalogSyncService
{
    Task<int> SyncFromWgerAsync(int limit = 30, CancellationToken cancellationToken = default);
}

public interface IExportService
{
    Task<ExportedFiles> ExportAsync(string outputDirectory, CancellationToken cancellationToken = default);
}
