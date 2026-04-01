using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class TemplateExerciseItemViewModel : ObservableObject
{
    private readonly Action<TemplateExerciseItemViewModel> _remove;

    [ObservableProperty]
    private string exerciseId = string.Empty;

    [ObservableProperty]
    private string exerciseName = string.Empty;

    [ObservableProperty]
    private string bodyPart = string.Empty;

    [ObservableProperty]
    private ExerciseCategory category;

    [ObservableProperty]
    private string notes = string.Empty;

    public TemplateExerciseItemViewModel(Action<TemplateExerciseItemViewModel> remove)
    {
        _remove = remove;
    }

    [RelayCommand]
    private void Remove() => _remove(this);

    public WorkoutTemplateExercise ToDomain(int sortOrder) =>
        new()
        {
            ExerciseId = ExerciseId,
            ExerciseName = ExerciseName,
            BodyPart = BodyPart,
            Category = Category,
            SortOrder = sortOrder,
            Notes = Notes
        };
}

public sealed partial class StrengthSetItemViewModel : ObservableObject
{
    private readonly Action<StrengthSetItemViewModel> _remove;

    [ObservableProperty]
    private string repsText = "0";

    [ObservableProperty]
    private string weightKgText = "0";

    [ObservableProperty]
    private string rirText = string.Empty;

    [ObservableProperty]
    private string rpeText = string.Empty;

    [ObservableProperty]
    private string notes = string.Empty;

    public string SetLabel { get; set; } = "Set 1";

    public StrengthSetItemViewModel(Action<StrengthSetItemViewModel> remove)
    {
        _remove = remove;
    }

    [RelayCommand]
    private void Remove() => _remove(this);

    public StrengthSet ToDomain(string workoutEntryId, int setNumber) =>
        new()
        {
            WorkoutEntryId = workoutEntryId,
            SetNumber = setNumber,
            Reps = ParseInt(RepsText),
            WeightKg = ParseDecimal(WeightKgText),
            Rir = ParseNullableDecimal(RirText),
            Rpe = ParseNullableDecimal(RpeText),
            Notes = Notes
        };

    public static StrengthSetItemViewModel FromDomain(StrengthSet set, Action<StrengthSetItemViewModel> remove) =>
        new(remove)
        {
            RepsText = set.Reps.ToString(),
            WeightKgText = set.WeightKg.ToString(),
            RirText = set.Rir?.ToString() ?? string.Empty,
            RpeText = set.Rpe?.ToString() ?? string.Empty,
            Notes = set.Notes
        };

    private static int ParseInt(string? value) => int.TryParse(value, out var parsed) ? parsed : 0;
    private static decimal ParseDecimal(string? value) => decimal.TryParse(value, out var parsed) ? parsed : 0;
    private static decimal? ParseNullableDecimal(string? value) => decimal.TryParse(value, out var parsed) ? parsed : null;
}

public sealed partial class WorkoutEntryItemViewModel : ObservableObject
{
    private readonly Action<WorkoutEntryItemViewModel> _remove;

    [ObservableProperty]
    private string exerciseId = string.Empty;

    [ObservableProperty]
    private string exerciseName = string.Empty;

    [ObservableProperty]
    private string bodyPart = string.Empty;

    [ObservableProperty]
    private ExerciseCategory category;

    [ObservableProperty]
    private string notes = string.Empty;

    [ObservableProperty]
    private string durationSecondsText = "0";

    [ObservableProperty]
    private string distanceKmText = string.Empty;

    [ObservableProperty]
    private string caloriesText = string.Empty;

    public ObservableCollection<StrengthSetItemViewModel> StrengthSets { get; } = [];
    public bool IsStrength => Category == ExerciseCategory.Strength;
    public bool IsCardio => !IsStrength;

    public WorkoutEntryItemViewModel(Action<WorkoutEntryItemViewModel> remove)
    {
        _remove = remove;
    }

    [RelayCommand]
    private void AddSet()
    {
        var item = new StrengthSetItemViewModel(RemoveSet) { SetLabel = $"Set {StrengthSets.Count + 1}" };
        StrengthSets.Add(item);
    }

    [RelayCommand]
    private void RemoveEntry() => _remove(this);

    private void RemoveSet(StrengthSetItemViewModel set)
    {
        StrengthSets.Remove(set);
        for (var index = 0; index < StrengthSets.Count; index++)
        {
            StrengthSets[index].SetLabel = $"Set {index + 1}";
        }
        OnPropertyChanged(nameof(StrengthSets));
    }

    public WorkoutEntry ToDomain(string sessionId, int sortOrder)
    {
        var entry = new WorkoutEntry
        {
            WorkoutSessionId = sessionId,
            ExerciseId = ExerciseId,
            ExerciseSnapshotName = ExerciseName,
            ExerciseSnapshotCategory = Category,
            ExerciseSnapshotPrimaryBodyPart = BodyPart,
            SortOrder = sortOrder,
            EntryType = Category,
            Notes = Notes
        };

        if (IsStrength)
        {
            for (var index = 0; index < StrengthSets.Count; index++)
            {
                entry.StrengthSets.Add(StrengthSets[index].ToDomain(entry.Id, index + 1));
            }
        }
        else
        {
            entry.CardioData = new CardioEntryData
            {
                WorkoutEntryId = entry.Id,
                DurationSeconds = int.TryParse(DurationSecondsText, out var seconds) ? seconds : 0,
                DistanceKm = decimal.TryParse(DistanceKmText, out var distance) ? distance : null,
                Calories = decimal.TryParse(CaloriesText, out var calories) ? calories : null,
                Notes = Notes
            };
        }

        return entry;
    }

    public static WorkoutEntryItemViewModel FromDomain(WorkoutEntry entry, Action<WorkoutEntryItemViewModel> remove)
    {
        var vm = new WorkoutEntryItemViewModel(remove)
        {
            ExerciseId = entry.ExerciseId,
            ExerciseName = entry.ExerciseSnapshotName,
            BodyPart = entry.ExerciseSnapshotPrimaryBodyPart,
            Category = entry.EntryType,
            Notes = entry.Notes,
            DurationSecondsText = entry.CardioData?.DurationSeconds.ToString() ?? "0",
            DistanceKmText = entry.CardioData?.DistanceKm?.ToString() ?? string.Empty,
            CaloriesText = entry.CardioData?.Calories?.ToString() ?? string.Empty
        };

        foreach (var (set, index) in entry.StrengthSets.Select((value, index) => (value, index)))
        {
            var setVm = StrengthSetItemViewModel.FromDomain(set, vm.RemoveSet);
            setVm.SetLabel = $"Set {index + 1}";
            vm.StrengthSets.Add(setVm);
        }

        if (vm.IsStrength && vm.StrengthSets.Count == 0)
        {
            vm.AddSet();
        }

        return vm;
    }
}
