using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class WorkoutEditorViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;
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

    public ObservableCollection<Exercise> AvailableExercises { get; } = [];
    public ObservableCollection<WorkoutEntryItemViewModel> Entries { get; } = [];

    public WorkoutEditorViewModel(IWorkoutDataService workoutDataService)
    {
        _workoutDataService = workoutDataService;
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
                AvailableExercises.Clear();
                foreach (var exercise in exercises)
                {
                    AvailableExercises.Add(exercise);
                }

                Entries.Clear();
                foreach (var entry in _currentSession.Entries.OrderBy(x => x.SortOrder))
                {
                    Entries.Add(WorkoutEntryItemViewModel.FromDomain(entry, RemoveEntry));
                }
                OnPropertyChanged(nameof(SessionId));
            });
            StartTimer();
            UpdateElapsed();
        });

    [RelayCommand]
    private void AddSelectedExercise()
    {
        if (SelectedExercise is null)
        {
            return;
        }

        Entries.Add(new WorkoutEntryItemViewModel(RemoveEntry)
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
            _currentSession.Status = WorkoutSessionStatus.Active;
            _currentSession = await _workoutDataService.SaveWorkoutSessionAsync(_currentSession).ConfigureAwait(false);
        }, "Workout draft saved.");

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
            _currentSession.EndedAtUtc = DateTime.UtcNow;
            _currentSession = await _workoutDataService.SaveWorkoutSessionAsync(_currentSession).ConfigureAwait(false);
            _timer?.Stop();
            await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync("///history"));
        }, "Workout completed.");

    [RelayCommand]
    private Task DiscardWorkoutAsync() =>
        RunBusyAsync(async () =>
        {
            if (_currentSession is null)
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

        var elapsed = DateTime.UtcNow - _currentSession.StartedAtUtc;
        ElapsedText = $"{(int)elapsed.TotalHours:00}:{elapsed.Minutes:00}:{elapsed.Seconds:00}";
    }

    private void RemoveEntry(WorkoutEntryItemViewModel entry)
    {
        Entries.Remove(entry);
    }
}
