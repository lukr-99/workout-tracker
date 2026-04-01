# Workout Tracker

Android-first workout tracking app built with `.NET MAUI` and `C#`.

## Current scope

- Offline-first workout logging
- Reusable workout templates
- Live workout sessions with a timer
- Strength and cardio entries
- Searchable exercise catalog with custom exercises
- Workout history and detail views
- CSV and JSON export for a future desktop manager
- Analytics-ready data model for later charts and progression views

## Solution layout

- `WorkoutTracker.App`: Android MAUI app, navigation, pages, and viewmodels
- `WorkoutTracker.Core`: domain models, SQLite repository, export, and sync services
- `WorkoutTracker.Tests`: repository-focused tests

## Current status

The app is in a functional v0.1 foundation state:

- the core data layer persists workouts locally with SQLite
- the main screens and flows are wired end to end
- export works through JSON and CSV generation
- history and exercise progression queries are available for future graphs

## Run and verify

```powershell
dotnet build WorkoutTracker.App\WorkoutTracker.App.csproj -f net9.0-android
dotnet test WorkoutTracker.Tests\WorkoutTracker.Tests.csproj
```

## Documentation

- `docs/decisions.md`
- `docs/roadmap.md`
