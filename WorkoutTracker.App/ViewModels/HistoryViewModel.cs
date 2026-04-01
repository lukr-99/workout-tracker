using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class HistoryViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;
    private List<WorkoutSessionSummary> _allWorkouts = [];

    [ObservableProperty]
    private string searchText = string.Empty;

    [ObservableProperty]
    private string selectedWorkoutType = "All";

    [ObservableProperty]
    private string selectedBodyPart = "All";

    [ObservableProperty]
    private bool isFiltersOpen;

    public ObservableCollection<string> WorkoutTypes { get; } = ["All", "Strength", "Cardio", "Mixed"];
    public ObservableCollection<string> BodyParts { get; } = ["All"];
    public ObservableCollection<WorkoutSessionSummary> Workouts { get; } = [];
    public string ResultsSummary => Workouts.Count == 0 ? "No workouts match the current filters." : $"{Workouts.Count} workout(s) shown";

    public HistoryViewModel(IWorkoutDataService workoutDataService)
    {
        _workoutDataService = workoutDataService;
        Title = "History";
    }

    public Task RefreshAsync() =>
        RunBusyAsync(async () =>
        {
            _allWorkouts = (await _workoutDataService.GetWorkoutHistoryAsync(SearchText).ConfigureAwait(false))
                .Select(workout =>
                {
                    workout.BodyPartsSummary = NormalizeBodyParts(workout.BodyPartsSummary)
                        .Replace("/", " / ", StringComparison.Ordinal)
                        .Replace("  /  ", " / ", StringComparison.Ordinal);
                    return workout;
                })
                .ToList();
            MainThread.BeginInvokeOnMainThread(ApplyFilters);
        });

    [RelayCommand]
    private Task SearchAsync() => RefreshAsync();

    [RelayCommand]
    private void ToggleFilters()
    {
        IsFiltersOpen = !IsFiltersOpen;
    }

    [RelayCommand]
    private void ClearFilters()
    {
        SelectedWorkoutType = "All";
        SelectedBodyPart = "All";
        ApplyFilters();
    }

    [RelayCommand]
    private async Task OpenWorkoutAsync(WorkoutSessionSummary? workout)
    {
        if (workout is null)
        {
            return;
        }

        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutDetailPage)}?sessionId={workout.Id}"));
    }

    partial void OnSelectedWorkoutTypeChanged(string value) => ApplyFilters();

    partial void OnSelectedBodyPartChanged(string value) => ApplyFilters();

    private void ApplyFilters()
    {
        var bodyParts = _allWorkouts
            .SelectMany(workout => NormalizeBodyParts(workout.BodyPartsSummary).Split('/', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries))
            .Where(x => !x.Equals("No body part tags", StringComparison.OrdinalIgnoreCase))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(x => x)
            .ToList();

        BodyParts.Clear();
        BodyParts.Add("All");
        foreach (var bodyPart in bodyParts)
        {
            BodyParts.Add(bodyPart);
        }

        if (!BodyParts.Contains(SelectedBodyPart))
        {
            SelectedBodyPart = "All";
        }

        var filtered = _allWorkouts
            .Where(workout => SelectedWorkoutType == "All" || workout.SessionTypeLabel.Equals(SelectedWorkoutType, StringComparison.OrdinalIgnoreCase))
            .Where(workout => SelectedBodyPart == "All" || workout.BodyPartsSummary.Contains(SelectedBodyPart, StringComparison.OrdinalIgnoreCase))
            .ToList();

        Workouts.Clear();
        foreach (var workout in filtered)
        {
            Workouts.Add(workout);
        }

        OnPropertyChanged(nameof(ResultsSummary));
    }

    private static string NormalizeBodyParts(string summary) =>
        summary.Replace("â€¢", "/", StringComparison.Ordinal).Replace("•", "/", StringComparison.Ordinal);
}
