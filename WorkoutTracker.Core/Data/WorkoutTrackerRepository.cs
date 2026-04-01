using System.Globalization;
using System.Text.Json;
using SQLite;
using WorkoutTracker.Core.Domain;
using WorkoutTracker.Core.Seed;
using WorkoutTracker.Core.Services;

namespace WorkoutTracker.Core.Data;

public sealed class WorkoutTrackerRepository : IWorkoutDataService, IWorkoutHistoryService, IAnalyticsService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly string _databasePath;
    private readonly SemaphoreSlim _initializationLock = new(1, 1);
    private SQLiteAsyncConnection? _database;

    public WorkoutTrackerRepository(string databasePath)
    {
        _databasePath = databasePath;
    }

    public async Task InitializeAsync(CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var existingCount = await db.Table<ExerciseRecord>().CountAsync().ConfigureAwait(false);
        if (existingCount == 0)
        {
            await db.InsertAllAsync(SeedExercises.Create().Select(ToRecord)).ConfigureAwait(false);
        }
    }

    public async Task<DashboardSnapshot> GetDashboardSnapshotAsync(CancellationToken cancellationToken = default)
    {
        return new DashboardSnapshot
        {
            ActiveWorkout = await GetActiveWorkoutAsync(cancellationToken).ConfigureAwait(false),
            RecentWorkouts = (await GetWorkoutHistoryAsync(cancellationToken: cancellationToken).ConfigureAwait(false)).Take(5).ToList(),
            Analytics = await GetAnalyticsOverviewAsync(cancellationToken).ConfigureAwait(false),
            Consistency = await GetConsistencySnapshotAsync(cancellationToken).ConfigureAwait(false)
        };
    }

    public async Task<IReadOnlyList<Exercise>> GetExercisesAsync(ExerciseFilter? filter = null, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        IEnumerable<ExerciseRecord> query = await db.Table<ExerciseRecord>().ToListAsync().ConfigureAwait(false);

        if (!(filter?.IncludeArchived ?? false))
        {
            query = query.Where(x => !x.IsArchived);
        }

        if (!string.IsNullOrWhiteSpace(filter?.SearchText))
        {
            var needle = filter.SearchText.Trim();
            query = query.Where(x =>
                x.Name.Contains(needle, StringComparison.OrdinalIgnoreCase) ||
                x.PrimaryBodyPart.Contains(needle, StringComparison.OrdinalIgnoreCase) ||
                x.Equipment.Contains(needle, StringComparison.OrdinalIgnoreCase));
        }

        if (!string.IsNullOrWhiteSpace(filter?.BodyPart))
        {
            var bodyPart = filter.BodyPart.Trim();
            query = query.Where(x =>
                x.PrimaryBodyPart.Equals(bodyPart, StringComparison.OrdinalIgnoreCase) ||
                DeserializeList(x.SecondaryBodyPartsJson).Any(y => y.Equals(bodyPart, StringComparison.OrdinalIgnoreCase)));
        }

        if (filter?.Category is not null)
        {
            query = query.Where(x => x.Category == filter.Category.Value);
        }

        return query
            .OrderBy(x => x.Category)
            .ThenBy(x => x.Name)
            .Select(ToDomain)
            .ToList();
    }

    public async Task<Exercise?> GetExerciseAsync(string id, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var record = await db.FindAsync<ExerciseRecord>(id).ConfigureAwait(false);
        return record is null ? null : ToDomain(record);
    }

    public async Task<Exercise> SaveExerciseAsync(Exercise exercise, CancellationToken cancellationToken = default)
    {
        NormalizeExercise(exercise);
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        await db.InsertOrReplaceAsync(ToRecord(exercise)).ConfigureAwait(false);
        return exercise;
    }

    public async Task ArchiveExerciseAsync(string id, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var record = await db.FindAsync<ExerciseRecord>(id).ConfigureAwait(false);
        if (record is null)
        {
            return;
        }

        record.IsArchived = true;
        await db.UpdateAsync(record).ConfigureAwait(false);
    }

    public async Task<int> MergeExternalExercisesAsync(IEnumerable<Exercise> exercises, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var existing = await db.Table<ExerciseRecord>().ToListAsync().ConfigureAwait(false);
        var byExternalId = existing
            .Where(x => !string.IsNullOrWhiteSpace(x.ExternalSourceId))
            .ToDictionary(x => x.ExternalSourceId!, StringComparer.OrdinalIgnoreCase);

        var merged = 0;
        foreach (var exercise in exercises)
        {
            NormalizeExercise(exercise);
            exercise.Source = ExerciseSource.Synced;
            if (!string.IsNullOrWhiteSpace(exercise.ExternalSourceId) &&
                byExternalId.TryGetValue(exercise.ExternalSourceId, out var match))
            {
                exercise.Id = match.Id;
            }

            await db.InsertOrReplaceAsync(ToRecord(exercise)).ConfigureAwait(false);
            merged++;
        }

        return merged;
    }

    public async Task<IReadOnlyList<WorkoutTemplate>> GetTemplatesAsync(CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var templateRecords = await db.Table<WorkoutTemplateRecord>().OrderBy(x => x.Name).ToListAsync().ConfigureAwait(false);
        var exerciseRecords = await db.Table<WorkoutTemplateExerciseRecord>().ToListAsync().ConfigureAwait(false);

        return templateRecords.Select(template => new WorkoutTemplate
        {
            Id = template.Id,
            Name = template.Name,
            Notes = template.Notes,
            Exercises = exerciseRecords
                .Where(x => x.TemplateId == template.Id)
                .OrderBy(x => x.SortOrder)
                .Select(ToDomain)
                .ToList()
        }).ToList();
    }

    public async Task<WorkoutTemplate?> GetTemplateAsync(string id, CancellationToken cancellationToken = default)
    {
        return (await GetTemplatesAsync(cancellationToken).ConfigureAwait(false)).FirstOrDefault(x => x.Id == id);
    }

    public async Task<WorkoutTemplate> SaveTemplateAsync(WorkoutTemplate template, CancellationToken cancellationToken = default)
    {
        template.Id = string.IsNullOrWhiteSpace(template.Id) ? Guid.NewGuid().ToString("N") : template.Id;
        template.Name = string.IsNullOrWhiteSpace(template.Name) ? "Untitled Template" : template.Name.Trim();
        var db = await GetDatabaseAsync().ConfigureAwait(false);

        await db.InsertOrReplaceAsync(new WorkoutTemplateRecord
        {
            Id = template.Id,
            Name = template.Name,
            Notes = template.Notes ?? string.Empty
        }).ConfigureAwait(false);

        await db.Table<WorkoutTemplateExerciseRecord>().DeleteAsync(x => x.TemplateId == template.Id).ConfigureAwait(false);
        var children = template.Exercises
            .OrderBy(x => x.SortOrder)
            .Select((exercise, index) =>
            {
                exercise.Id = string.IsNullOrWhiteSpace(exercise.Id) ? Guid.NewGuid().ToString("N") : exercise.Id;
                exercise.SortOrder = index;
                return new WorkoutTemplateExerciseRecord
                {
                    Id = exercise.Id,
                    TemplateId = template.Id,
                    ExerciseId = exercise.ExerciseId,
                    ExerciseName = exercise.ExerciseName,
                    Category = exercise.Category,
                    BodyPart = exercise.BodyPart,
                    SortOrder = exercise.SortOrder,
                    Notes = exercise.Notes ?? string.Empty
                };
            })
            .ToList();

        if (children.Count > 0)
        {
            await db.InsertAllAsync(children).ConfigureAwait(false);
        }

        return template;
    }

    public async Task DeleteTemplateAsync(string id, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        await db.Table<WorkoutTemplateExerciseRecord>().DeleteAsync(x => x.TemplateId == id).ConfigureAwait(false);
        await db.DeleteAsync<WorkoutTemplateRecord>(id).ConfigureAwait(false);
    }

    public async Task<WorkoutSession> CreateWorkoutSessionAsync(string? templateId = null, string? name = null, CancellationToken cancellationToken = default)
    {
        var active = await GetActiveWorkoutAsync(cancellationToken).ConfigureAwait(false);
        if (active is not null)
        {
            return active;
        }

        var session = new WorkoutSession
        {
            Id = Guid.NewGuid().ToString("N"),
            TemplateId = templateId,
            Name = string.IsNullOrWhiteSpace(name) ? "Quick Workout" : name.Trim(),
            StartedAtUtc = DateTime.UtcNow,
            Status = WorkoutSessionStatus.Active
        };

        if (!string.IsNullOrWhiteSpace(templateId))
        {
            var template = await GetTemplateAsync(templateId, cancellationToken).ConfigureAwait(false);
            if (template is not null)
            {
                session.Name = string.IsNullOrWhiteSpace(name) ? template.Name : name!.Trim();
                session.Entries = template.Exercises
                    .OrderBy(x => x.SortOrder)
                    .Select((exercise, index) => new WorkoutEntry
                    {
                        WorkoutSessionId = session.Id,
                        ExerciseId = exercise.ExerciseId,
                        ExerciseSnapshotName = exercise.ExerciseName,
                        ExerciseSnapshotCategory = exercise.Category,
                        ExerciseSnapshotPrimaryBodyPart = exercise.BodyPart,
                        SortOrder = index,
                        EntryType = exercise.Category,
                        Notes = exercise.Notes,
                        StrengthSets = exercise.Category == ExerciseCategory.Strength ? [new StrengthSet { SetNumber = 1 }] : [],
                        CardioData = exercise.Category == ExerciseCategory.Cardio ? new CardioEntryData() : null
                    })
                    .ToList();
            }
        }

        return await SaveWorkoutSessionAsync(session, cancellationToken).ConfigureAwait(false);
    }

    public async Task<WorkoutSession?> GetActiveWorkoutAsync(CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var record = await db.Table<WorkoutSessionRecord>()
            .Where(x => x.Status == WorkoutSessionStatus.Active)
            .OrderByDescending(x => x.StartedAtUtc)
            .FirstOrDefaultAsync()
            .ConfigureAwait(false);

        return record is null ? null : await LoadSessionAsync(record).ConfigureAwait(false);
    }

    public async Task<WorkoutSession?> GetWorkoutSessionAsync(string id, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var record = await db.FindAsync<WorkoutSessionRecord>(id).ConfigureAwait(false);
        return record is null ? null : await LoadSessionAsync(record).ConfigureAwait(false);
    }

    public async Task<WorkoutSession> SaveWorkoutSessionAsync(WorkoutSession session, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        session.Id = string.IsNullOrWhiteSpace(session.Id) ? Guid.NewGuid().ToString("N") : session.Id;
        session.Name = string.IsNullOrWhiteSpace(session.Name) ? "Quick Workout" : session.Name.Trim();
        session.Notes ??= string.Empty;

        if (session.Status == WorkoutSessionStatus.Completed)
        {
            session.EndedAtUtc ??= DateTime.UtcNow;
            session.CompletedDateUtc ??= session.EndedAtUtc;
            session.DurationSeconds = session.DurationSeconds > 0
                ? session.DurationSeconds
                : (long)Math.Max(0, ((session.EndedAtUtc ?? DateTime.UtcNow) - session.StartedAtUtc).TotalSeconds);
        }

        await db.InsertOrReplaceAsync(ToRecord(session)).ConfigureAwait(false);
        await ReplaceSessionChildrenAsync(db, session).ConfigureAwait(false);
        return await GetWorkoutSessionAsync(session.Id, cancellationToken).ConfigureAwait(false) ?? session;
    }

    public async Task DeleteWorkoutSessionAsync(string id, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var entries = await db.Table<WorkoutEntryRecord>().Where(x => x.WorkoutSessionId == id).ToListAsync().ConfigureAwait(false);
        foreach (var entry in entries)
        {
            await db.Table<StrengthSetRecord>().DeleteAsync(x => x.WorkoutEntryId == entry.Id).ConfigureAwait(false);
            await db.DeleteAsync<CardioEntryRecord>(entry.Id).ConfigureAwait(false);
        }

        await db.Table<WorkoutEntryRecord>().DeleteAsync(x => x.WorkoutSessionId == id).ConfigureAwait(false);
        await db.DeleteAsync<WorkoutSessionRecord>(id).ConfigureAwait(false);
    }

    public async Task<IReadOnlyList<WorkoutSessionSummary>> GetWorkoutHistoryAsync(string? searchText = null, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var sessions = await db.Table<WorkoutSessionRecord>()
            .Where(x => x.Status == WorkoutSessionStatus.Completed)
            .OrderByDescending(x => x.CompletedDateUtc)
            .ToListAsync()
            .ConfigureAwait(false);

        if (!string.IsNullOrWhiteSpace(searchText))
        {
            sessions = sessions.Where(x => x.Name.Contains(searchText.Trim(), StringComparison.OrdinalIgnoreCase)).ToList();
        }

        var entries = await db.Table<WorkoutEntryRecord>().ToListAsync().ConfigureAwait(false);
        var sets = await db.Table<StrengthSetRecord>().ToListAsync().ConfigureAwait(false);
        var cardio = await db.Table<CardioEntryRecord>().ToListAsync().ConfigureAwait(false);
        var entriesBySession = entries.GroupBy(x => x.WorkoutSessionId).ToDictionary(x => x.Key, x => x.ToList());
        var setsByEntry = sets.GroupBy(x => x.WorkoutEntryId).ToDictionary(x => x.Key, x => x.ToList());
        var cardioByEntry = cardio.ToDictionary(x => x.WorkoutEntryId, x => x);

        return sessions.Select(session => new WorkoutSessionSummary
        {
            Id = session.Id,
            Name = session.Name,
            StartedAtUtc = session.StartedAtUtc,
            CompletedDateUtc = session.CompletedDateUtc,
            Status = session.Status,
            ExerciseCount = entriesBySession.GetValueOrDefault(session.Id, []).Count,
            StrengthSetCount = entriesBySession.GetValueOrDefault(session.Id, []).Sum(x => setsByEntry.GetValueOrDefault(x.Id, []).Count),
            TotalVolumeKg = entriesBySession.GetValueOrDefault(session.Id, []).Sum(x => setsByEntry.GetValueOrDefault(x.Id, []).Sum(y => y.WeightKg * y.Reps)),
            CardioMinutes = entriesBySession.GetValueOrDefault(session.Id, []).Sum(x => cardioByEntry.TryGetValue(x.Id, out var cardioEntry) ? cardioEntry.DurationSeconds / 60 : 0)
        }).ToList();
    }

    public async Task<WorkoutTemplate> DuplicateWorkoutAsTemplateAsync(string sessionId, string? templateName = null, CancellationToken cancellationToken = default)
    {
        var session = await GetWorkoutSessionAsync(sessionId, cancellationToken).ConfigureAwait(false)
            ?? throw new InvalidOperationException("Workout session not found.");

        var template = new WorkoutTemplate
        {
            Name = string.IsNullOrWhiteSpace(templateName) ? $"{session.Name} Copy" : templateName.Trim(),
            Notes = $"Created from workout on {(session.CompletedDateUtc ?? session.StartedAtUtc).ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)}",
            Exercises = session.Entries
                .OrderBy(x => x.SortOrder)
                .Where(x => !string.IsNullOrWhiteSpace(x.ExerciseId))
                .Select((entry, index) => new WorkoutTemplateExercise
                {
                    ExerciseId = entry.ExerciseId,
                    ExerciseName = entry.ExerciseSnapshotName,
                    Category = entry.EntryType,
                    BodyPart = entry.ExerciseSnapshotPrimaryBodyPart,
                    SortOrder = index,
                    Notes = entry.Notes
                })
                .ToList()
        };

        return await SaveTemplateAsync(template, cancellationToken).ConfigureAwait(false);
    }

    public async Task<ExportBundle> CreateExportBundleAsync(CancellationToken cancellationToken = default)
    {
        var exercises = await GetExercisesAsync(new ExerciseFilter { IncludeArchived = true }, cancellationToken).ConfigureAwait(false);
        var templates = await GetTemplatesAsync(cancellationToken).ConfigureAwait(false);
        var summaries = await GetWorkoutHistoryAsync(cancellationToken: cancellationToken).ConfigureAwait(false);
        var sessions = new List<WorkoutSession>();

        foreach (var summary in summaries)
        {
            var session = await GetWorkoutSessionAsync(summary.Id, cancellationToken).ConfigureAwait(false);
            if (session is not null)
            {
                sessions.Add(session);
            }
        }

        var active = await GetActiveWorkoutAsync(cancellationToken).ConfigureAwait(false);
        if (active is not null && sessions.All(x => x.Id != active.Id))
        {
            sessions.Add(active);
        }

        return new ExportBundle
        {
            Exercises = exercises.ToList(),
            Templates = templates.ToList(),
            Sessions = sessions.OrderByDescending(x => x.StartedAtUtc).ToList()
        };
    }

    public async Task<AnalyticsOverview> GetAnalyticsOverviewAsync(CancellationToken cancellationToken = default)
    {
        var summaries = await GetWorkoutHistoryAsync(cancellationToken: cancellationToken).ConfigureAwait(false);
        var recentCutoff = DateTime.UtcNow.AddDays(-30);
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var entries = await db.Table<WorkoutEntryRecord>().ToListAsync().ConfigureAwait(false);

        return new AnalyticsOverview
        {
            TotalCompletedWorkouts = summaries.Count,
            WorkoutsLast30Days = summaries.Count(x => (x.CompletedDateUtc ?? x.StartedAtUtc) >= recentCutoff),
            TotalVolumeKg = summaries.Sum(x => x.TotalVolumeKg),
            CurrentWeeklyStreak = CalculateWeeklyStreak(summaries.Select(x => x.CompletedDateUtc ?? x.StartedAtUtc)),
            MostLoggedExerciseName = entries
                .GroupBy(x => x.ExerciseSnapshotName)
                .OrderByDescending(x => x.Count())
                .Select(x => x.Key)
                .FirstOrDefault() ?? string.Empty
        };
    }

    public async Task<WorkoutConsistencySnapshot> GetConsistencySnapshotAsync(CancellationToken cancellationToken = default)
    {
        var dates = (await GetWorkoutHistoryAsync(cancellationToken: cancellationToken).ConfigureAwait(false))
            .Select(x => (x.CompletedDateUtc ?? x.StartedAtUtc).Date)
            .Distinct()
            .OrderBy(x => x)
            .ToList();

        var today = DateTime.UtcNow.Date;
        var longestGap = 0;
        for (var index = 1; index < dates.Count; index++)
        {
            longestGap = Math.Max(longestGap, (dates[index] - dates[index - 1]).Days);
        }

        return new WorkoutConsistencySnapshot
        {
            WorkoutsLast7Days = dates.Count(x => x >= today.AddDays(-7)),
            WorkoutsLast30Days = dates.Count(x => x >= today.AddDays(-30)),
            CurrentWeeklyStreak = CalculateWeeklyStreak(dates),
            LongestGapDays = longestGap
        };
    }

    public async Task<IReadOnlyList<ExerciseAnalyticsPoint>> GetExerciseProgressAsync(string exerciseId, CancellationToken cancellationToken = default)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var sessions = await db.Table<WorkoutSessionRecord>()
            .Where(x => x.Status == WorkoutSessionStatus.Completed)
            .OrderBy(x => x.CompletedDateUtc)
            .ToListAsync()
            .ConfigureAwait(false);
        var entries = await db.Table<WorkoutEntryRecord>().Where(x => x.ExerciseId == exerciseId).ToListAsync().ConfigureAwait(false);
        var sets = await db.Table<StrengthSetRecord>().ToListAsync().ConfigureAwait(false);
        var entriesBySession = entries.GroupBy(x => x.WorkoutSessionId).ToDictionary(x => x.Key, x => x.ToList());
        var setsByEntry = sets.GroupBy(x => x.WorkoutEntryId).ToDictionary(x => x.Key, x => x.ToList());
        var points = new List<ExerciseAnalyticsPoint>();

        foreach (var session in sessions)
        {
            if (!entriesBySession.TryGetValue(session.Id, out var sessionEntries))
            {
                continue;
            }

            var sessionSets = sessionEntries.SelectMany(x => setsByEntry.GetValueOrDefault(x.Id, [])).ToList();
            if (sessionSets.Count == 0)
            {
                continue;
            }

            points.Add(new ExerciseAnalyticsPoint
            {
                ExerciseId = exerciseId,
                DateUtc = session.CompletedDateUtc ?? session.StartedAtUtc,
                BestWeightKg = sessionSets.Max(x => x.WeightKg),
                TotalVolumeKg = sessionSets.Sum(x => x.WeightKg * x.Reps),
                TotalReps = sessionSets.Sum(x => x.Reps),
                SessionName = session.Name
            });
        }

        return points;
    }

    private async Task<SQLiteAsyncConnection> GetDatabaseAsync()
    {
        if (_database is not null)
        {
            return _database;
        }

        await _initializationLock.WaitAsync().ConfigureAwait(false);
        try
        {
            if (_database is not null)
            {
                return _database;
            }

            SQLitePCL.Batteries_V2.Init();
            _database = new SQLiteAsyncConnection(
                _databasePath,
                SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.Create | SQLiteOpenFlags.SharedCache);

            await _database.CreateTableAsync<ExerciseRecord>().ConfigureAwait(false);
            await _database.CreateTableAsync<WorkoutTemplateRecord>().ConfigureAwait(false);
            await _database.CreateTableAsync<WorkoutTemplateExerciseRecord>().ConfigureAwait(false);
            await _database.CreateTableAsync<WorkoutSessionRecord>().ConfigureAwait(false);
            await _database.CreateTableAsync<WorkoutEntryRecord>().ConfigureAwait(false);
            await _database.CreateTableAsync<StrengthSetRecord>().ConfigureAwait(false);
            await _database.CreateTableAsync<CardioEntryRecord>().ConfigureAwait(false);

            return _database;
        }
        finally
        {
            _initializationLock.Release();
        }
    }

    private async Task ReplaceSessionChildrenAsync(SQLiteAsyncConnection db, WorkoutSession session)
    {
        var oldEntries = await db.Table<WorkoutEntryRecord>().Where(x => x.WorkoutSessionId == session.Id).ToListAsync().ConfigureAwait(false);
        foreach (var oldEntry in oldEntries)
        {
            await db.Table<StrengthSetRecord>().DeleteAsync(x => x.WorkoutEntryId == oldEntry.Id).ConfigureAwait(false);
            await db.DeleteAsync<CardioEntryRecord>(oldEntry.Id).ConfigureAwait(false);
        }

        await db.Table<WorkoutEntryRecord>().DeleteAsync(x => x.WorkoutSessionId == session.Id).ConfigureAwait(false);

        var entries = new List<WorkoutEntryRecord>();
        var sets = new List<StrengthSetRecord>();
        var cardio = new List<CardioEntryRecord>();

        foreach (var (entry, index) in session.Entries.OrderBy(x => x.SortOrder).Select((value, index) => (value, index)))
        {
            entry.Id = string.IsNullOrWhiteSpace(entry.Id) ? Guid.NewGuid().ToString("N") : entry.Id;
            entry.WorkoutSessionId = session.Id;
            entry.SortOrder = index;
            entries.Add(ToRecord(entry));

            if (entry.EntryType == ExerciseCategory.Strength)
            {
                foreach (var (set, setIndex) in entry.StrengthSets.Select((value, setIndex) => (value, setIndex)))
                {
                    set.Id = string.IsNullOrWhiteSpace(set.Id) ? Guid.NewGuid().ToString("N") : set.Id;
                    set.WorkoutEntryId = entry.Id;
                    set.SetNumber = setIndex + 1;
                    sets.Add(ToRecord(set));
                }
            }
            else
            {
                var cardioEntry = entry.CardioData ?? new CardioEntryData();
                cardioEntry.WorkoutEntryId = entry.Id;
                cardio.Add(ToRecord(cardioEntry));
            }
        }

        if (entries.Count > 0)
        {
            await db.InsertAllAsync(entries).ConfigureAwait(false);
        }

        if (sets.Count > 0)
        {
            await db.InsertAllAsync(sets).ConfigureAwait(false);
        }

        if (cardio.Count > 0)
        {
            await db.InsertAllAsync(cardio).ConfigureAwait(false);
        }
    }

    private async Task<WorkoutSession> LoadSessionAsync(WorkoutSessionRecord record)
    {
        var db = await GetDatabaseAsync().ConfigureAwait(false);
        var entries = await db.Table<WorkoutEntryRecord>().Where(x => x.WorkoutSessionId == record.Id).OrderBy(x => x.SortOrder).ToListAsync().ConfigureAwait(false);
        var sets = await db.Table<StrengthSetRecord>().ToListAsync().ConfigureAwait(false);
        var cardio = await db.Table<CardioEntryRecord>().ToListAsync().ConfigureAwait(false);
        var setsByEntry = sets.GroupBy(x => x.WorkoutEntryId).ToDictionary(x => x.Key, x => x.OrderBy(y => y.SetNumber).ToList());
        var cardioByEntry = cardio.ToDictionary(x => x.WorkoutEntryId, x => x);

        return new WorkoutSession
        {
            Id = record.Id,
            TemplateId = record.TemplateId,
            Name = record.Name,
            StartedAtUtc = record.StartedAtUtc,
            EndedAtUtc = record.EndedAtUtc,
            CompletedDateUtc = record.CompletedDateUtc,
            DurationSeconds = record.DurationSeconds,
            Notes = record.Notes,
            Status = record.Status,
            Entries = entries.Select(entry => new WorkoutEntry
            {
                Id = entry.Id,
                WorkoutSessionId = entry.WorkoutSessionId,
                ExerciseId = entry.ExerciseId,
                ExerciseSnapshotName = entry.ExerciseSnapshotName,
                ExerciseSnapshotCategory = entry.ExerciseSnapshotCategory,
                ExerciseSnapshotPrimaryBodyPart = entry.ExerciseSnapshotPrimaryBodyPart,
                SortOrder = entry.SortOrder,
                EntryType = entry.EntryType,
                Notes = entry.Notes,
                StrengthSets = setsByEntry.GetValueOrDefault(entry.Id, []).Select(ToDomain).ToList(),
                CardioData = cardioByEntry.TryGetValue(entry.Id, out var cardioEntry) ? ToDomain(cardioEntry) : null
            }).ToList()
        };
    }

    private static int CalculateWeeklyStreak(IEnumerable<DateTime> dates)
    {
        var buckets = dates.Select(x => (ISOWeek.GetYear(x), ISOWeek.GetWeekOfYear(x))).Distinct().ToHashSet();
        if (buckets.Count == 0)
        {
            return 0;
        }

        var streak = 0;
        var cursor = GetWeekStart(DateTime.UtcNow);
        while (buckets.Contains((ISOWeek.GetYear(cursor), ISOWeek.GetWeekOfYear(cursor))))
        {
            streak++;
            cursor = cursor.AddDays(-7);
        }

        return streak;
    }

    private static DateTime GetWeekStart(DateTime dateUtc)
    {
        var diff = ((int)dateUtc.DayOfWeek + 6) % 7;
        return dateUtc.Date.AddDays(-diff);
    }

    private static void NormalizeExercise(Exercise exercise)
    {
        exercise.Id = string.IsNullOrWhiteSpace(exercise.Id) ? Guid.NewGuid().ToString("N") : exercise.Id;
        exercise.Name = string.IsNullOrWhiteSpace(exercise.Name) ? "Custom Exercise" : exercise.Name.Trim();
        exercise.PrimaryBodyPart = string.IsNullOrWhiteSpace(exercise.PrimaryBodyPart)
            ? (exercise.Category == ExerciseCategory.Cardio ? "Cardio" : "Full Body")
            : exercise.PrimaryBodyPart.Trim();
        exercise.Equipment ??= string.Empty;
        exercise.Notes ??= string.Empty;
        exercise.SecondaryBodyParts = exercise.SecondaryBodyParts
            .Where(x => !string.IsNullOrWhiteSpace(x))
            .Select(x => x.Trim())
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    private static ExerciseRecord ToRecord(Exercise exercise) =>
        new()
        {
            Id = exercise.Id,
            Name = exercise.Name,
            Category = exercise.Category,
            PrimaryBodyPart = exercise.PrimaryBodyPart,
            SecondaryBodyPartsJson = JsonSerializer.Serialize(exercise.SecondaryBodyParts, JsonOptions),
            Equipment = exercise.Equipment,
            Notes = exercise.Notes,
            Source = exercise.Source,
            ExternalSourceId = exercise.ExternalSourceId,
            IsArchived = exercise.IsArchived
        };

    private static Exercise ToDomain(ExerciseRecord record) =>
        new()
        {
            Id = record.Id,
            Name = record.Name,
            Category = record.Category,
            PrimaryBodyPart = record.PrimaryBodyPart,
            SecondaryBodyParts = DeserializeList(record.SecondaryBodyPartsJson),
            Equipment = record.Equipment,
            Notes = record.Notes,
            Source = record.Source,
            ExternalSourceId = record.ExternalSourceId,
            IsArchived = record.IsArchived
        };

    private static WorkoutTemplateExercise ToDomain(WorkoutTemplateExerciseRecord record) =>
        new()
        {
            Id = record.Id,
            ExerciseId = record.ExerciseId,
            ExerciseName = record.ExerciseName,
            Category = record.Category,
            BodyPart = record.BodyPart,
            SortOrder = record.SortOrder,
            Notes = record.Notes
        };

    private static WorkoutSessionRecord ToRecord(WorkoutSession session) =>
        new()
        {
            Id = session.Id,
            TemplateId = session.TemplateId,
            Name = session.Name,
            Status = session.Status,
            StartedAtUtc = session.StartedAtUtc,
            EndedAtUtc = session.EndedAtUtc,
            CompletedDateUtc = session.CompletedDateUtc,
            DurationSeconds = session.DurationSeconds,
            Notes = session.Notes
        };

    private static WorkoutEntryRecord ToRecord(WorkoutEntry entry) =>
        new()
        {
            Id = entry.Id,
            WorkoutSessionId = entry.WorkoutSessionId,
            ExerciseId = entry.ExerciseId,
            ExerciseSnapshotName = string.IsNullOrWhiteSpace(entry.ExerciseSnapshotName) ? "Exercise" : entry.ExerciseSnapshotName,
            ExerciseSnapshotCategory = entry.ExerciseSnapshotCategory,
            ExerciseSnapshotPrimaryBodyPart = entry.ExerciseSnapshotPrimaryBodyPart,
            SortOrder = entry.SortOrder,
            EntryType = entry.EntryType,
            Notes = entry.Notes ?? string.Empty
        };

    private static StrengthSetRecord ToRecord(StrengthSet set) =>
        new()
        {
            Id = set.Id,
            WorkoutEntryId = set.WorkoutEntryId,
            SetNumber = set.SetNumber,
            Reps = set.Reps,
            WeightKg = set.WeightKg,
            Rir = set.Rir,
            Rpe = set.Rpe,
            PerformedAtUtc = set.PerformedAtUtc,
            Notes = set.Notes ?? string.Empty
        };

    private static StrengthSet ToDomain(StrengthSetRecord record) =>
        new()
        {
            Id = record.Id,
            WorkoutEntryId = record.WorkoutEntryId,
            SetNumber = record.SetNumber,
            Reps = record.Reps,
            WeightKg = record.WeightKg,
            Rir = record.Rir,
            Rpe = record.Rpe,
            PerformedAtUtc = record.PerformedAtUtc,
            Notes = record.Notes
        };

    private static CardioEntryRecord ToRecord(CardioEntryData cardio) =>
        new()
        {
            WorkoutEntryId = cardio.WorkoutEntryId,
            DurationSeconds = cardio.DurationSeconds,
            DistanceKm = cardio.DistanceKm,
            Calories = cardio.Calories,
            Notes = cardio.Notes ?? string.Empty
        };

    private static CardioEntryData ToDomain(CardioEntryRecord record) =>
        new()
        {
            WorkoutEntryId = record.WorkoutEntryId,
            DurationSeconds = record.DurationSeconds,
            DistanceKm = record.DistanceKm,
            Calories = record.Calories,
            Notes = record.Notes
        };

    private static List<string> DeserializeList(string? json)
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            return [];
        }

        try
        {
            return JsonSerializer.Deserialize<List<string>>(json, JsonOptions) ?? [];
        }
        catch
        {
            return [];
        }
    }
}
