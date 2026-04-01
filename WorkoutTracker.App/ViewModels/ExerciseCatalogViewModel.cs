using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.App.Services;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class ExerciseCatalogViewModel : BaseViewModel
{
    private static readonly string[] DefaultBodyPartOptions =
    [
        "Full Body",
        "Chest",
        "Back",
        "Shoulders",
        "Biceps",
        "Triceps",
        "Forearms",
        "Core",
        "Glutes",
        "Quads",
        "Hamstrings",
        "Calves",
        "Cardio"
    ];

    private readonly IWorkoutDataService _workoutDataService;
    private readonly IExerciseCatalogSyncService _syncService;
    private readonly IAppDialogService _dialogService;
    private string? _currentExerciseId;
    private ExerciseSource _currentExerciseSource = ExerciseSource.Custom;
    private string? _currentExternalSourceId;
    private bool _currentExerciseArchived;

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
    private string selectedSecondaryBodyPart = string.Empty;

    [ObservableProperty]
    private string equipment = string.Empty;

    [ObservableProperty]
    private string notes = string.Empty;

    [ObservableProperty]
    private string editorCategory = nameof(ExerciseCategory.Strength);

    [ObservableProperty]
    private bool isEditorOpen;

    [ObservableProperty]
    private bool isAdvancedFiltersOpen;

    public ObservableCollection<string> BodyParts { get; } = ["All"];
    public ObservableCollection<string> Categories { get; } = ["All", nameof(ExerciseCategory.Strength), nameof(ExerciseCategory.Cardio)];
    public ObservableCollection<string> EditorCategories { get; } = [nameof(ExerciseCategory.Strength), nameof(ExerciseCategory.Cardio)];
    public ObservableCollection<string> EditorBodyParts { get; } = [];
    public ObservableCollection<string> SelectedSecondaryBodyParts { get; } = [];
    public ObservableCollection<Exercise> Exercises { get; } = [];
    public string EditorTitle => string.IsNullOrWhiteSpace(_currentExerciseId) ? "New exercise" : "Edit exercise";
    public bool HasSecondaryBodyParts => SelectedSecondaryBodyParts.Count > 0;

    public ExerciseCatalogViewModel(IWorkoutDataService workoutDataService, IExerciseCatalogSyncService syncService, IAppDialogService dialogService)
    {
        _workoutDataService = workoutDataService;
        _syncService = syncService;
        _dialogService = dialogService;
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
                .SelectMany(x => new[] { x.PrimaryBodyPart }.Concat(x.SecondaryBodyParts))
                .Where(x => !string.IsNullOrWhiteSpace(x))
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

                RefreshEditorBodyParts(bodyParts);
            });
        });

    [RelayCommand]
    private void NewExercise()
    {
        _currentExerciseId = null;
        _currentExerciseSource = ExerciseSource.Custom;
        _currentExternalSourceId = null;
        _currentExerciseArchived = false;
        ExerciseName = string.Empty;
        PrimaryBodyPart = "Full Body";
        SelectedSecondaryBodyPart = string.Empty;
        SelectedSecondaryBodyParts.Clear();
        Equipment = string.Empty;
        Notes = string.Empty;
        EditorCategory = nameof(ExerciseCategory.Strength);
        OnPropertyChanged(nameof(HasSecondaryBodyParts));
        IsEditorOpen = true;
        OnPropertyChanged(nameof(EditorTitle));
    }

    [RelayCommand]
    private void EditExercise(Exercise? exercise)
    {
        if (exercise is null)
        {
            return;
        }

        _currentExerciseId = exercise.Id;
        _currentExerciseSource = exercise.Source;
        _currentExternalSourceId = exercise.ExternalSourceId;
        _currentExerciseArchived = exercise.IsArchived;
        ExerciseName = exercise.Name;
        PrimaryBodyPart = exercise.PrimaryBodyPart;
        SelectedSecondaryBodyPart = string.Empty;
        SelectedSecondaryBodyParts.Clear();
        foreach (var bodyPart in exercise.SecondaryBodyParts)
        {
            SelectedSecondaryBodyParts.Add(bodyPart);
        }
        Equipment = exercise.Equipment;
        Notes = exercise.Notes;
        EditorCategory = exercise.Category.ToString();
        OnPropertyChanged(nameof(HasSecondaryBodyParts));
        IsEditorOpen = true;
        OnPropertyChanged(nameof(EditorTitle));
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
                SecondaryBodyParts = SelectedSecondaryBodyParts.ToList(),
                Equipment = Equipment,
                Notes = Notes,
                Category = Enum.Parse<ExerciseCategory>(EditorCategory),
                Source = _currentExerciseSource,
                ExternalSourceId = _currentExternalSourceId,
                IsArchived = _currentExerciseArchived
            };

            await _workoutDataService.SaveExerciseAsync(exercise).ConfigureAwait(false);
            _currentExerciseId = exercise.Id;
            await RefreshAsync().ConfigureAwait(false);
            MainThread.BeginInvokeOnMainThread(() => IsEditorOpen = false);
        }, "Exercise saved.");

    [RelayCommand]
    private Task ArchiveExerciseAsync(Exercise? exercise) =>
        RunBusyAsync(async () =>
        {
            if (exercise is null)
            {
                return;
            }

            var confirmed = await _dialogService
                .ConfirmAsync("Archive exercise", $"Archive \"{exercise.Name}\" from the active catalog?", "Archive", "Cancel")
                .ConfigureAwait(false);
            if (!confirmed)
            {
                return;
            }

            await _workoutDataService.ArchiveExerciseAsync(exercise.Id).ConfigureAwait(false);
            await RefreshAsync().ConfigureAwait(false);
        }, "Exercise archived.");

    [RelayCommand]
    private Task SearchAsync() => RefreshAsync();

    [RelayCommand]
    private void ToggleAdvancedFilters()
    {
        IsAdvancedFiltersOpen = !IsAdvancedFiltersOpen;
    }

    [RelayCommand]
    private void CloseEditor()
    {
        IsEditorOpen = false;
    }

    [RelayCommand]
    private void AddSecondaryBodyPart()
    {
        if (string.IsNullOrWhiteSpace(SelectedSecondaryBodyPart))
        {
            return;
        }

        if (SelectedSecondaryBodyPart.Equals(PrimaryBodyPart, StringComparison.OrdinalIgnoreCase))
        {
            SelectedSecondaryBodyPart = string.Empty;
            return;
        }

        if (SelectedSecondaryBodyParts.Any(x => x.Equals(SelectedSecondaryBodyPart, StringComparison.OrdinalIgnoreCase)))
        {
            SelectedSecondaryBodyPart = string.Empty;
            return;
        }

        SelectedSecondaryBodyParts.Add(SelectedSecondaryBodyPart);
        SelectedSecondaryBodyPart = string.Empty;
        OnPropertyChanged(nameof(HasSecondaryBodyParts));
    }

    [RelayCommand]
    private void RemoveSecondaryBodyPart(string? bodyPart)
    {
        if (string.IsNullOrWhiteSpace(bodyPart))
        {
            return;
        }

        var existing = SelectedSecondaryBodyParts.FirstOrDefault(x => x.Equals(bodyPart, StringComparison.OrdinalIgnoreCase));
        if (existing is null)
        {
            return;
        }

        SelectedSecondaryBodyParts.Remove(existing);
        OnPropertyChanged(nameof(HasSecondaryBodyParts));
    }

    [RelayCommand]
    private Task SyncAsync() =>
        RunBusyAsync(async () =>
        {
            await _syncService.SyncFromWgerAsync(20).ConfigureAwait(false);
            await RefreshAsync().ConfigureAwait(false);
        }, "Exercise ideas imported from the public wger catalog.");

    partial void OnPrimaryBodyPartChanged(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return;
        }

        var duplicate = SelectedSecondaryBodyParts.FirstOrDefault(x => x.Equals(value, StringComparison.OrdinalIgnoreCase));
        if (duplicate is null)
        {
            return;
        }

        SelectedSecondaryBodyParts.Remove(duplicate);
        OnPropertyChanged(nameof(HasSecondaryBodyParts));
    }

    partial void OnEditorCategoryChanged(string value)
    {
        if (string.IsNullOrWhiteSpace(PrimaryBodyPart))
        {
            PrimaryBodyPart = value == nameof(ExerciseCategory.Cardio) ? "Cardio" : "Full Body";
            return;
        }

        if (value == nameof(ExerciseCategory.Cardio) && PrimaryBodyPart.Equals("Full Body", StringComparison.OrdinalIgnoreCase))
        {
            PrimaryBodyPart = "Cardio";
        }
    }

    private void RefreshEditorBodyParts(IEnumerable<string> bodyParts)
    {
        var options = DefaultBodyPartOptions
            .Concat(bodyParts)
            .Concat([PrimaryBodyPart, SelectedSecondaryBodyPart])
            .Concat(SelectedSecondaryBodyParts)
            .Where(x => !string.IsNullOrWhiteSpace(x))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(x => x)
            .ToList();

        EditorBodyParts.Clear();
        foreach (var option in options)
        {
            EditorBodyParts.Add(option);
        }

        if (string.IsNullOrWhiteSpace(PrimaryBodyPart))
        {
            PrimaryBodyPart = EditorCategory == nameof(ExerciseCategory.Cardio) ? "Cardio" : "Full Body";
        }
    }
}
