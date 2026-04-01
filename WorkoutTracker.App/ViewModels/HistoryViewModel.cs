using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class HistoryViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;

    [ObservableProperty]
    private string searchText = string.Empty;

    public ObservableCollection<WorkoutSessionSummary> Workouts { get; } = [];

    public HistoryViewModel(IWorkoutDataService workoutDataService)
    {
        _workoutDataService = workoutDataService;
        Title = "History";
    }

    public Task RefreshAsync() =>
        RunBusyAsync(async () =>
        {
            var items = await _workoutDataService.GetWorkoutHistoryAsync(SearchText).ConfigureAwait(false);
            MainThread.BeginInvokeOnMainThread(() =>
            {
                Workouts.Clear();
                foreach (var item in items)
                {
                    Workouts.Add(item);
                }
            });
        });

    [RelayCommand]
    private Task SearchAsync() => RefreshAsync();

    [RelayCommand]
    private async Task OpenWorkoutAsync(WorkoutSessionSummary? workout)
    {
        if (workout is null)
        {
            return;
        }

        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutDetailPage)}?sessionId={workout.Id}"));
    }
}
