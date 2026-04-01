using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.App.Services;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class WorkoutEditorViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;
    private readonly IAppDialogService _dialogService;
    private IDispatcherTimer? _timer;
    private WorkoutSession? _currentSession;

    [ObservableProperty]
    private string sessionId = string.Empty;

    [ObservableProperty]
    private string sessionName = string.Empty;

    [ObservableProperty]
    private string notes = string.Empty;

    [ObservableProperty]
    private string elapsedText = "00:00:00";

    [ObservableProperty]
    private Exercise? selectedExercise;

    [ObservableProperty]
    private string exerciseSearchText = string.Empty;

    [ObservableProperty]
    private bool isHistoricalWorkout;

    public string PrimaryActionText => IsHistoricalWorkout ? "Save changes" : "Finish workout";
    public string SecondaryActionText => IsHistoricalWorkout ? "Save as completed" : "Save draft";
    public bool CanDiscardWorkout => !IsHistoricalWorkout;

    public ObservableCollection<Exercise> AvailableExercises { get; } = [];
    public ObservableCollection<Exercise> FilteredExercises { get; } = [];
    public ObservableCollection<WorkoutEntryItemViewModel> Entries { get; } = [];

    public WorkoutEditorViewModel(IWorkoutDataService workoutDataService, IAppDialogService dialogService)
    {
        _workoutDataService = workoutDataService;
        _dialogService = dialogService;
        Title = "Live Workout";
    }

    public Task LoadAsync(string sessionId) =>
        RunBusyAsync(async () =>
        {
            _currentSession = await _workoutDataService.GetWorkoutSessionAsync(sessionId).ConfigureAwait(false)
                ?? await _workoutDataService.CreateWorkoutSessionAsync().ConfigureAwait(false);

            var exercises = await _workoutDataService.GetExercisesAsync().ConfigureAwait(false);
            MainThread.BeginInvokeOnMainThread(() =>
            {
                SessionId = _currentSession.Id;
                SessionName = _currentSession.Name;
                Notes = _currentSession.Notes;
                IsHistoricalWorkout = _currentSession.Status == WorkoutSessionStatus.Completed;
                AvailableExercises.Clear();
                foreach (var exercise in exercises)
                {
                    AvailableExercises.Add(exercise);
                }
                UpdateExerciseSuggestions();

                Entries.Clear();
                foreach (var entry in _currentSession.Entries.OrderBy(x => x.SortOrder))
                {
                    Entries.Add(WorkoutEntryItemViewModel.FromDomain(entry, RemoveEntryAsync));
                }
                OnPropertyChanged(nameof(SessionId));
                OnPropertyChanged(nameof(PrimaryActionText));
                OnPropertyChanged(nameof(SecondaryActionText));
                OnPropertyChanged(nameof(CanDiscardWorkout));
            });
            if (IsHistoricalWorkout)
            {
                _timer?.Stop();
            }
            else
            {
                StartTimer();
            }
            UpdateElapsed();
        });

    [RelayCommand]
    private void AddSelectedExercise()
    {
        if (SelectedExercise is null)
        {
            return;
        }

        Entries.Add(new WorkoutEntryItemViewModel(RemoveEntryAsync)
        {
            ExerciseId = SelectedExercise.Id,
            ExerciseName = SelectedExercise.Name,
            BodyPart = SelectedExercise.PrimaryBodyPart,
            Category = SelectedExercise.Category,
            DurationSecondsText = "0"
        });

        if (SelectedExercise.Category == ExerciseCategory.Strength)
        {
            Entries.Last().AddSetCommand.Execute(null);
        }

        SelectedExercise = null;
        ExerciseSearchText = string.Empty;
    }

    [RelayCommand]
    private void AddExerciseFromSuggestion(Exercise? exercise)
    {
        if (exercise is null)
        {
            return;
        }

        SelectedExercise = exercise;
        AddSelectedExercise();
    }

    [RelayCommand]
    private Task SaveDraftAsync() =>
        RunBusyAsync(async () =>
        {
            if (_currentSession is null)
            {
                return;
            }

            _currentSession.Name = SessionName;
            _currentSession.Notes = Notes;
            _currentSession.Entries = Entries.Select((entry, index) => entry.ToDomain(_currentSession.Id, index)).ToList();
            _currentSession.Status = IsHistoricalWorkout ? WorkoutSessionStatus.Completed : WorkoutSessionStatus.Active;
            _currentSession = await _workoutDataService.SaveWorkoutSessionAsync(_currentSession).ConfigureAwait(false);
        }, IsHistoricalWorkout ? "Completed workout updated." : "Workout draft saved.");

    [RelayCommand]
    private Task FinishWorkoutAsync() =>
        RunBusyAsync(async () =>
        {
            if (_currentSession is null)
            {
                return;
            }

            _currentSession.Name = SessionName;
            _currentSession.Notes = Notes;
            _currentSession.Entries = Entries.Select((entry, index) => entry.ToDomain(_currentSession.Id, index)).ToList();
            _currentSession.Status = WorkoutSessionStatus.Completed;
            _currentSession.EndedAtUtc = _currentSession.EndedAtUtc ?? DateTime.UtcNow;
            _currentSession.CompletedDateUtc = _currentSession.CompletedDateUtc ?? _currentSession.EndedAtUtc;
            _currentSession = await _workoutDataService.SaveWorkoutSessionAsync(_currentSession).ConfigureAwait(false);
            _timer?.Stop();
            await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync("///home"));
        }, IsHistoricalWorkout ? "Workout changes saved." : "Workout completed.");

    [RelayCommand]
    private Task DiscardWorkoutAsync() =>
        RunBusyAsync(async () =>
        {
            if (_currentSession is null)
            {
                return;
            }

            var confirmed = await _dialogService
                .ConfirmAsync("Discard workout", "Discard the active workout and remove its unsaved progress?", "Discard", "Keep editing")
                .ConfigureAwait(false);
            if (!confirmed)
            {
                return;
            }

            await _workoutDataService.DeleteWorkoutSessionAsync(_currentSession.Id).ConfigureAwait(false);
            _timer?.Stop();
            await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync("///home"));
        }, "Workout discarded.");

    public void StopTimer()
    {
        _timer?.Stop();
    }

    private void StartTimer()
    {
        _timer?.Stop();
        _timer = Application.Current?.Dispatcher.CreateTimer();
        if (_timer is null)
        {
            return;
        }

        _timer.Interval = TimeSpan.FromSeconds(1);
        _timer.Tick += (_, _) => UpdateElapsed();
        _timer.Start();
    }

    private void UpdateElapsed()
    {
        if (_currentSession is null)
        {
            ElapsedText = "00:00:00";
            return;
        }

        var elapsed = IsHistoricalWorkout
            ? (_currentSession.EndedAtUtc ?? _currentSession.CompletedDateUtc ?? _currentSession.StartedAtUtc) - _currentSession.StartedAtUtc
            : DateTime.UtcNow - _currentSession.StartedAtUtc;
        ElapsedText = $"{(int)elapsed.TotalHours:00}:{elapsed.Minutes:00}:{elapsed.Seconds:00}";
    }

    partial void OnExerciseSearchTextChanged(string value)
    {
        UpdateExerciseSuggestions();
    }

    private async Task RemoveEntryAsync(WorkoutEntryItemViewModel entry)
    {
        var confirmed = await _dialogService
            .ConfirmAsync("Remove exercise", $"Remove {entry.ExerciseName} from this workout?", "Remove", "Cancel")
            .ConfigureAwait(false);
        if (!confirmed)
        {
            return;
        }

        Entries.Remove(entry);
    }

    private void UpdateExerciseSuggestions()
    {
        var filtered = AvailableExercises
            .Where(exercise =>
                string.IsNullOrWhiteSpace(ExerciseSearchText)
                || exercise.Name.Contains(ExerciseSearchText, StringComparison.OrdinalIgnoreCase)
                || exercise.PrimaryBodyPart.Contains(ExerciseSearchText, StringComparison.OrdinalIgnoreCase))
            .OrderBy(exercise => exercise.Name)
            .ToList();

        FilteredExercises.Clear();
        foreach (var exercise in filtered)
        {
            FilteredExercises.Add(exercise);
        }

        if (SelectedExercise is not null && !FilteredExercises.Contains(SelectedExercise))
        {
            SelectedExercise = null;
        }
    }
}
