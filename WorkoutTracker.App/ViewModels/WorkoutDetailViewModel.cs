using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class WorkoutDetailExerciseItemViewModel : ObservableObject
{
    public string Name { get; init; } = string.Empty;
    public string Subtitle { get; init; } = string.Empty;
    public ObservableCollection<string> Lines { get; } = [];
}

public sealed partial class WorkoutDetailViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;
    private readonly IAnalyticsService _analyticsService;
    private WorkoutSession? _currentSession;

    public ObservableCollection<WorkoutDetailExerciseItemViewModel> Entries { get; } = [];
    public string SessionHeader { get; private set; } = string.Empty;
    public string Notes { get; private set; } = string.Empty;
    public string ProgressSummary { get; private set; } = string.Empty;

    public WorkoutDetailViewModel(IWorkoutDataService workoutDataService, IAnalyticsService analyticsService)
    {
        _workoutDataService = workoutDataService;
        _analyticsService = analyticsService;
        Title = "Workout Detail";
    }

    public Task LoadAsync(string sessionId) =>
        RunBusyAsync(async () =>
        {
            _currentSession = await _workoutDataService.GetWorkoutSessionAsync(sessionId).ConfigureAwait(false);
            if (_currentSession is null)
            {
                return;
            }

            var progressText = new List<string>();
            var items = new List<WorkoutDetailExerciseItemViewModel>();
            foreach (var entry in _currentSession.Entries.OrderBy(x => x.SortOrder))
            {
                var item = new WorkoutDetailExerciseItemViewModel
                {
                    Name = entry.ExerciseSnapshotName,
                    Subtitle = $"{entry.ExerciseSnapshotPrimaryBodyPart} | {entry.EntryType}"
                };

                if (entry.EntryType == ExerciseCategory.Strength)
                {
                    foreach (var set in entry.StrengthSets.OrderBy(x => x.SetNumber))
                    {
                        item.Lines.Add($"Set {set.SetNumber}: {set.Reps} reps, {set.WeightKg} kg");
                    }

                    var analytics = await _analyticsService.GetExerciseProgressAsync(entry.ExerciseId).ConfigureAwait(false);
                    if (analytics.Count > 0)
                    {
                        progressText.Add($"{entry.ExerciseSnapshotName}: {analytics.Count} logged point(s), best {analytics.Max(x => x.BestWeightKg)} kg");
                    }
                }
                else if (entry.CardioData is not null)
                {
                    item.Lines.Add($"Duration: {entry.CardioData.DurationSeconds / 60} min");
                    if (entry.CardioData.DistanceKm is not null)
                    {
                        item.Lines.Add($"Distance: {entry.CardioData.DistanceKm} km");
                    }
                    if (entry.CardioData.Calories is not null)
                    {
                        item.Lines.Add($"Calories: {entry.CardioData.Calories}");
                    }
                }

                items.Add(item);
            }

            MainThread.BeginInvokeOnMainThread(() =>
            {
                SessionHeader = $"{_currentSession.Name} | {(_currentSession.CompletedDateUtc ?? _currentSession.StartedAtUtc):yyyy-MM-dd HH:mm}";
                Notes = _currentSession.Notes;
                ProgressSummary = progressText.Count == 0 ? "No progression points yet." : string.Join(Environment.NewLine, progressText);
                Entries.Clear();
                foreach (var item in items)
                {
                    Entries.Add(item);
                }
                OnPropertyChanged(nameof(SessionHeader));
                OnPropertyChanged(nameof(Notes));
                OnPropertyChanged(nameof(ProgressSummary));
            });
        });

    [RelayCommand]
    private async Task DuplicateAsTemplateAsync()
    {
        if (_currentSession is null)
        {
            return;
        }

        await RunBusyAsync(async () =>
        {
            await _workoutDataService.DuplicateWorkoutAsTemplateAsync(_currentSession.Id).ConfigureAwait(false);
        }, "Workout duplicated as a template.");
    }

    [RelayCommand]
    private async Task StartCopyAsync()
    {
        if (_currentSession is null)
        {
            return;
        }

        var template = await _workoutDataService.DuplicateWorkoutAsTemplateAsync(_currentSession.Id, $"{_currentSession.Name} Repeat").ConfigureAwait(false);
        var session = await _workoutDataService.CreateWorkoutSessionAsync(template.Id).ConfigureAwait(false);
        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutEditorPage)}?sessionId={session.Id}"));
    }

    [RelayCommand]
    private async Task EditWorkoutAsync()
    {
        if (_currentSession is null)
        {
            return;
        }

        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutEditorPage)}?sessionId={_currentSession.Id}"));
    }
}
