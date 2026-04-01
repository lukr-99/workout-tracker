using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class ExerciseCatalogViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;
    private readonly IExerciseCatalogSyncService _syncService;
    private string? _currentExerciseId;

    [ObservableProperty]
    private string searchText = string.Empty;

    [ObservableProperty]
    private string selectedBodyPart = "All";

    [ObservableProperty]
    private string selectedCategory = "All";

    [ObservableProperty]
    private string exerciseName = string.Empty;

    [ObservableProperty]
    private string primaryBodyPart = string.Empty;

    [ObservableProperty]
    private string secondaryBodyParts = string.Empty;

    [ObservableProperty]
    private string equipment = string.Empty;

    [ObservableProperty]
    private string notes = string.Empty;

    [ObservableProperty]
    private string editorCategory = nameof(ExerciseCategory.Strength);

    public ObservableCollection<string> BodyParts { get; } = ["All"];
    public ObservableCollection<string> Categories { get; } = ["All", nameof(ExerciseCategory.Strength), nameof(ExerciseCategory.Cardio)];
    public ObservableCollection<Exercise> Exercises { get; } = [];

    public ExerciseCatalogViewModel(IWorkoutDataService workoutDataService, IExerciseCatalogSyncService syncService)
    {
        _workoutDataService = workoutDataService;
        _syncService = syncService;
        Title = "Catalog";
    }

    public Task RefreshAsync() =>
        RunBusyAsync(async () =>
        {
            var filter = new ExerciseFilter
            {
                SearchText = SearchText,
                BodyPart = SelectedBodyPart == "All" ? string.Empty : SelectedBodyPart,
                Category = SelectedCategory == "All" ? null : Enum.Parse<ExerciseCategory>(SelectedCategory)
            };

            var exercises = await _workoutDataService.GetExercisesAsync(filter).ConfigureAwait(false);
            var bodyParts = (await _workoutDataService.GetExercisesAsync().ConfigureAwait(false))
                .Select(x => x.PrimaryBodyPart)
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .OrderBy(x => x)
                .ToList();

            MainThread.BeginInvokeOnMainThread(() =>
            {
                Exercises.Clear();
                foreach (var exercise in exercises)
                {
                    Exercises.Add(exercise);
                }

                BodyParts.Clear();
                BodyParts.Add("All");
                foreach (var bodyPart in bodyParts)
                {
                    BodyParts.Add(bodyPart);
                }
            });
        });

    [RelayCommand]
    private void NewExercise()
    {
        _currentExerciseId = null;
        ExerciseName = string.Empty;
        PrimaryBodyPart = string.Empty;
        SecondaryBodyParts = string.Empty;
        Equipment = string.Empty;
        Notes = string.Empty;
        EditorCategory = nameof(ExerciseCategory.Strength);
    }

    [RelayCommand]
    private void EditExercise(Exercise? exercise)
    {
        if (exercise is null)
        {
            return;
        }

        _currentExerciseId = exercise.Id;
        ExerciseName = exercise.Name;
        PrimaryBodyPart = exercise.PrimaryBodyPart;
        SecondaryBodyParts = string.Join(", ", exercise.SecondaryBodyParts);
        Equipment = exercise.Equipment;
        Notes = exercise.Notes;
        EditorCategory = exercise.Category.ToString();
    }

    [RelayCommand]
    private Task SaveExerciseAsync() =>
        RunBusyAsync(async () =>
        {
            var exercise = new Exercise
            {
                Id = _currentExerciseId ?? Guid.NewGuid().ToString("N"),
                Name = ExerciseName,
                PrimaryBodyPart = PrimaryBodyPart,
                SecondaryBodyParts = SecondaryBodyParts.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries).ToList(),
                Equipment = Equipment,
                Notes = Notes,
                Category = Enum.Parse<ExerciseCategory>(EditorCategory),
                Source = ExerciseSource.Custom
            };

            await _workoutDataService.SaveExerciseAsync(exercise).ConfigureAwait(false);
            _currentExerciseId = exercise.Id;
            await RefreshAsync().ConfigureAwait(false);
        }, "Exercise saved.");

    [RelayCommand]
    private Task ArchiveExerciseAsync(Exercise? exercise) =>
        RunBusyAsync(async () =>
        {
            if (exercise is null)
            {
                return;
            }

            await _workoutDataService.ArchiveExerciseAsync(exercise.Id).ConfigureAwait(false);
            await RefreshAsync().ConfigureAwait(false);
        }, "Exercise archived.");

    [RelayCommand]
    private Task SearchAsync() => RefreshAsync();

    [RelayCommand]
    private Task SyncAsync() =>
        RunBusyAsync(async () =>
        {
            await _syncService.SyncFromWgerAsync(20).ConfigureAwait(false);
            await RefreshAsync().ConfigureAwait(false);
        }, "Catalog synced from wger.");
}
