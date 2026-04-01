using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class HomeViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;

    public ObservableCollection<WorkoutSessionSummary> RecentWorkouts { get; } = [];

    public string ActiveWorkoutName { get; private set; } = "No active workout";
    public string ActiveWorkoutSummary { get; private set; } = "Start a quick session or use a template.";
    public bool HasActiveWorkout { get; private set; }
    public string AnalyticsSummary { get; private set; } = string.Empty;
    public string ConsistencySummary { get; private set; } = string.Empty;
    public string MostLoggedExercise { get; private set; } = string.Empty;
    public string? ActiveWorkoutId { get; private set; }

    public HomeViewModel(IWorkoutDataService workoutDataService)
    {
        _workoutDataService = workoutDataService;
        Title = "Home";
    }

    public Task RefreshAsync() =>
        RunBusyAsync(async () =>
        {
            var dashboard = await _workoutDataService.GetDashboardSnapshotAsync().ConfigureAwait(false);
            MainThread.BeginInvokeOnMainThread(() =>
            {
                RecentWorkouts.Clear();
                foreach (var workout in dashboard.RecentWorkouts)
                {
                    RecentWorkouts.Add(workout);
                }

                HasActiveWorkout = dashboard.ActiveWorkout is not null;
                ActiveWorkoutId = dashboard.ActiveWorkout?.Id;
                ActiveWorkoutName = dashboard.ActiveWorkout?.Name ?? "No active workout";
                ActiveWorkoutSummary = dashboard.ActiveWorkout is null
                    ? "Start a quick session or use a template."
                    : $"{dashboard.ActiveWorkout.Entries.Count} exercises in progress";
                AnalyticsSummary = $"{dashboard.Analytics.TotalCompletedWorkouts} workouts logged | {dashboard.Analytics.TotalVolumeKg:N0} kg total volume";
                ConsistencySummary = $"{dashboard.Consistency.WorkoutsLast7Days} workout days in the last 7 days | streak {dashboard.Consistency.CurrentWeeklyStreak} week(s)";
                MostLoggedExercise = string.IsNullOrWhiteSpace(dashboard.Analytics.MostLoggedExerciseName)
                    ? "Most logged exercise will appear here."
                    : $"Most logged exercise: {dashboard.Analytics.MostLoggedExerciseName}";
                OnPropertyChanged(nameof(HasActiveWorkout));
                OnPropertyChanged(nameof(ActiveWorkoutName));
                OnPropertyChanged(nameof(ActiveWorkoutSummary));
                OnPropertyChanged(nameof(AnalyticsSummary));
                OnPropertyChanged(nameof(ConsistencySummary));
                OnPropertyChanged(nameof(MostLoggedExercise));
            });
        });

    [RelayCommand]
    private async Task StartQuickWorkoutAsync()
    {
        if (IsBusy)
        {
            return;
        }

        var session = await _workoutDataService.CreateWorkoutSessionAsync().ConfigureAwait(false);
        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutEditorPage)}?sessionId={session.Id}"));
    }

    [RelayCommand]
    private async Task ContinueWorkoutAsync()
    {
        if (string.IsNullOrWhiteSpace(ActiveWorkoutId))
        {
            return;
        }

        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutEditorPage)}?sessionId={ActiveWorkoutId}"));
    }

    [RelayCommand]
    private async Task OpenWorkoutAsync(WorkoutSessionSummary? summary)
    {
        if (summary is null)
        {
            return;
        }

        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutDetailPage)}?sessionId={summary.Id}"));
    }
}
