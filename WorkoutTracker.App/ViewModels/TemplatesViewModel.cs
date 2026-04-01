using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using WorkoutTracker.App.Services;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.App.ViewModels;

public sealed partial class TemplatesViewModel : BaseViewModel
{
    private readonly IWorkoutDataService _workoutDataService;
    private readonly IAppDialogService _dialogService;

    private string? _currentTemplateId;

    [ObservableProperty]
    private string templateName = string.Empty;

    [ObservableProperty]
    private string templateNotes = string.Empty;

    [ObservableProperty]
    private Exercise? selectedExercise;

    [ObservableProperty]
    private string exerciseSearchText = string.Empty;

    public ObservableCollection<WorkoutTemplate> Templates { get; } = [];
    public ObservableCollection<Exercise> AvailableExercises { get; } = [];
    public ObservableCollection<Exercise> FilteredExercises { get; } = [];
    public ObservableCollection<TemplateExerciseItemViewModel> CurrentExercises { get; } = [];

    public TemplatesViewModel(IWorkoutDataService workoutDataService, IAppDialogService dialogService)
    {
        _workoutDataService = workoutDataService;
        _dialogService = dialogService;
        Title = "Templates";
    }

    public Task RefreshAsync() =>
        RunBusyAsync(async () =>
        {
            var templates = await _workoutDataService.GetTemplatesAsync().ConfigureAwait(false);
            var exercises = await _workoutDataService.GetExercisesAsync().ConfigureAwait(false);
            MainThread.BeginInvokeOnMainThread(() =>
            {
                Templates.Clear();
                AvailableExercises.Clear();
                foreach (var template in templates)
                {
                    Templates.Add(template);
                }
                foreach (var exercise in exercises)
                {
                    AvailableExercises.Add(exercise);
                }
                UpdateExerciseSuggestions();
            });
        });

    [RelayCommand]
    private void NewTemplate()
    {
        _currentTemplateId = null;
        TemplateName = string.Empty;
        TemplateNotes = string.Empty;
        CurrentExercises.Clear();
    }

    [RelayCommand]
    private void AddExercise()
    {
        if (SelectedExercise is null)
        {
            return;
        }

        CurrentExercises.Add(new TemplateExerciseItemViewModel(RemoveExercise)
        {
            ExerciseId = SelectedExercise.Id,
            ExerciseName = SelectedExercise.Name,
            BodyPart = SelectedExercise.PrimaryBodyPart,
            Category = SelectedExercise.Category
        });

        SelectedExercise = null;
        ExerciseSearchText = string.Empty;
    }

    [RelayCommand]
    private Task SaveTemplateAsync() =>
        RunBusyAsync(async () =>
        {
            var template = new WorkoutTemplate
            {
                Id = _currentTemplateId ?? Guid.NewGuid().ToString("N"),
                Name = TemplateName,
                Notes = TemplateNotes,
                Exercises = CurrentExercises.Select((exercise, index) => exercise.ToDomain(index)).ToList()
            };

            await _workoutDataService.SaveTemplateAsync(template).ConfigureAwait(false);
            _currentTemplateId = template.Id;
            await RefreshAsync().ConfigureAwait(false);
        }, "Template saved.");

    [RelayCommand]
    private void EditTemplate(WorkoutTemplate? template)
    {
        if (template is null)
        {
            return;
        }

        _currentTemplateId = template.Id;
        TemplateName = template.Name;
        TemplateNotes = template.Notes;
        CurrentExercises.Clear();
        foreach (var exercise in template.Exercises.OrderBy(x => x.SortOrder))
        {
            CurrentExercises.Add(new TemplateExerciseItemViewModel(RemoveExercise)
            {
                ExerciseId = exercise.ExerciseId,
                ExerciseName = exercise.ExerciseName,
                BodyPart = exercise.BodyPart,
                Category = exercise.Category,
                Notes = exercise.Notes
            });
        }
    }

    [RelayCommand]
    private Task DeleteTemplateAsync(WorkoutTemplate? template) =>
        RunBusyAsync(async () =>
        {
            if (template is null)
            {
                return;
            }

            var confirmed = await _dialogService
                .ConfirmAsync("Delete template", $"Delete the template \"{template.Name}\"?", "Delete", "Cancel")
                .ConfigureAwait(false);
            if (!confirmed)
            {
                return;
            }

            await _workoutDataService.DeleteTemplateAsync(template.Id).ConfigureAwait(false);
            if (_currentTemplateId == template.Id)
            {
                NewTemplate();
            }
            await RefreshAsync().ConfigureAwait(false);
        }, "Template deleted.");

    [RelayCommand]
    private async Task StartTemplateAsync(WorkoutTemplate? template)
    {
        if (template is null)
        {
            return;
        }

        var session = await _workoutDataService.CreateWorkoutSessionAsync(template.Id).ConfigureAwait(false);
        await MainThread.InvokeOnMainThreadAsync(() => Shell.Current.GoToAsync($"{nameof(Pages.WorkoutEditorPage)}?sessionId={session.Id}"));
    }

    private void RemoveExercise(TemplateExerciseItemViewModel exercise)
    {
        CurrentExercises.Remove(exercise);
    }

    partial void OnExerciseSearchTextChanged(string value)
    {
        UpdateExerciseSuggestions();
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
