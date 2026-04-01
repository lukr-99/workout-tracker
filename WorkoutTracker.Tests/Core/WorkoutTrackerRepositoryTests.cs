using FluentAssertions;
using WorkoutTracker.Core.Data;
using WorkoutTracker.Core.Domain;

namespace WorkoutTracker.Tests.Core;

public sealed class WorkoutTrackerRepositoryTests : IDisposable
{
    private readonly string _databasePath;
    private readonly WorkoutTrackerRepository _repository;

    public WorkoutTrackerRepositoryTests()
    {
        _databasePath = Path.Combine(Path.GetTempPath(), $"workout-tracker-tests-{Guid.NewGuid():N}.db3");
        _repository = new WorkoutTrackerRepository(_databasePath);
        _repository.InitializeAsync().GetAwaiter().GetResult();
    }

    [Fact]
    public async Task InitializeAsync_SeedsExercises_AndSupportsFiltering()
    {
        var allExercises = await _repository.GetExercisesAsync();
        var cardioExercises = await _repository.GetExercisesAsync(new ExerciseFilter { Category = ExerciseCategory.Cardio });

        allExercises.Should().NotBeEmpty();
        cardioExercises.Should().NotBeEmpty();
        cardioExercises.Should().OnlyContain(x => x.Category == ExerciseCategory.Cardio);
    }

    [Fact]
    public async Task SaveTemplate_AndCreateSession_PreservesExerciseSnapshots()
    {
        var exercises = await _repository.GetExercisesAsync();
        var chosen = exercises.Take(2).ToList();

        var template = await _repository.SaveTemplateAsync(new WorkoutTemplate
        {
            Name = "Push Day",
            Exercises =
            [
                new WorkoutTemplateExercise
                {
                    ExerciseId = chosen[0].Id,
                    ExerciseName = chosen[0].Name,
                    BodyPart = chosen[0].PrimaryBodyPart,
                    Category = chosen[0].Category,
                    SortOrder = 0
                },
                new WorkoutTemplateExercise
                {
                    ExerciseId = chosen[1].Id,
                    ExerciseName = chosen[1].Name,
                    BodyPart = chosen[1].PrimaryBodyPart,
                    Category = chosen[1].Category,
                    SortOrder = 1
                }
            ]
        });

        var session = await _repository.CreateWorkoutSessionAsync(template.Id);

        session.Name.Should().Be("Push Day");
        session.Entries.Should().HaveCount(2);
        session.Entries[0].ExerciseSnapshotName.Should().Be(chosen[0].Name);
        session.Entries[0].ExerciseSnapshotPrimaryBodyPart.Should().Be(chosen[0].PrimaryBodyPart);
    }

    [Fact]
    public async Task CompletedWorkout_IsIncludedInHistoryAnalytics_AndExport()
    {
        var exercise = await _repository.SaveExerciseAsync(new Exercise
        {
            Name = "Test Press",
            PrimaryBodyPart = "Chest",
            Category = ExerciseCategory.Strength,
            Source = ExerciseSource.Custom
        });

        var session = await _repository.CreateWorkoutSessionAsync(name: "Analytics Day");
        session.Entries =
        [
            new WorkoutEntry
            {
                ExerciseId = exercise.Id,
                ExerciseSnapshotName = exercise.Name,
                ExerciseSnapshotPrimaryBodyPart = exercise.PrimaryBodyPart,
                ExerciseSnapshotCategory = exercise.Category,
                EntryType = ExerciseCategory.Strength,
                StrengthSets =
                [
                    new StrengthSet { SetNumber = 1, Reps = 5, WeightKg = 60 },
                    new StrengthSet { SetNumber = 2, Reps = 5, WeightKg = 65 }
                ]
            }
        ];
        session.Status = WorkoutSessionStatus.Completed;
        session.EndedAtUtc = session.StartedAtUtc.AddMinutes(40);

        await _repository.SaveWorkoutSessionAsync(session);

        var history = await _repository.GetWorkoutHistoryAsync();
        var analytics = await _repository.GetAnalyticsOverviewAsync();
        var progress = await _repository.GetExerciseProgressAsync(exercise.Id);
        var export = await _repository.CreateExportBundleAsync();

        history.Should().ContainSingle(x => x.Name == "Analytics Day");
        analytics.TotalCompletedWorkouts.Should().BeGreaterThanOrEqualTo(1);
        progress.Should().ContainSingle();
        progress[0].BestWeightKg.Should().Be(65);
        export.Sessions.Should().Contain(x => x.Name == "Analytics Day");
    }

    public void Dispose()
    {
        try
        {
            if (File.Exists(_databasePath))
            {
                File.Delete(_databasePath);
            }
        }
        catch (IOException)
        {
        }
    }
}
