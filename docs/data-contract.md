# Persistent data contract

What Ember stores, where, in which format, how it is backed up, and how it is deleted. CodePrint
requires this for every app that holds user data (`RULES.md`, Data lifecycle).

## What is stored

| Data | Owner | Where | Format and version | In the backup |
|---|---|---|---|---|
| Exercises, templates, workouts, sets | `WorkoutRepository` | Room `workout.db` | Room schema 8 (`app/schemas/`) | Yes |
| Runs, trace points, routes | `RunRepository` | Room `workout.db` | Room schema 8 | Yes |
| Settings: theme, units, default rest | `SettingsStore` | Preferences DataStore `settings` | Keys `theme_mode`, `units`, `default_rest_seconds` | Yes |
| Personal exercise photos | `ExercisePhotoStore` | `files/exercise_images/` | JPEG, path in `Exercise.localImagePath` | Yes, scaled to 1600 px |
| Automatic backup setup | `BackupScheduler` | Its own DataStore | Folder URI, interval, retention | No, it belongs to one phone |
| A live run in progress | `RunSessionController` | Crash buffer in app files | Internal | No, it is saved as a run when it ends |
| Offline map tiles | `OfflineTileCache` | MapLibre cache | MapLibre | No, it can be downloaded again |

The owner of each row is the only class that writes it. Data sent to Health Connect is owned by
Health Connect once written.

## The backup file

A backup is one JSON file, the `ExportBundle` (`data/export/ExportBundle.kt`):

- `exportFormatVersion` is `1.9`. Every version from `1.0` on still imports.
- Template exercises carry an optional plan (from 1.9): `targetSets`, `repsMin`, `repsMax`,
  `restSeconds` and `supersetGroup`.
- `exportedAtUtc` and `appVersion` (from 1.8) say when it was made and by which build.
- Ids are stable strings. Timestamps are ISO-8601 UTC. Weights are kilograms, distances are
  metres. Enums are ordinals, except in `settings`, where they are names.
- Photos are Base64 JPEG, scaled down to at most 1600 px on the long side.

**Save JSON** on the Data screen, the share sheet, and automatic backup all write this file. CSV
export is a spreadsheet of workouts only, not a backup.

## Backup paths

- **Manual:** Settings, Data, Save JSON or Share JSON. The file goes where the owner chooses,
  outside the app.
- **Automatic:** daily or weekly into a folder the owner picks (Storage Access Framework), as
  `workout-backup-<UTC time>.json`. The newest N files are kept; only files with that name pattern
  are ever deleted.

## Restore

Every import is read, checked and planned before anything is written. The preview shows the counts
and any problems; a file with errors cannot be committed. Files over 64 MB are refused.

- **Merge** (default): adds what is new and merges what matches by id, external id or name. Nothing
  on the phone is removed, settings are not changed, and a photo the phone already has is kept.
- **Replace everything** (Ember backups only): after a confirmation that names what will be deleted,
  everything is deleted and the backup is restored exactly, settings and photos included.

A commit is atomic. Photos are written to new files first. One database transaction then writes
every row, runs and routes included. If anything fails, the new photo files are deleted and the
database is unchanged. A photo path that only existed on another phone is cleared.

## Migrations

Room migrations are additive and run in order. There is no destructive fallback. Each one has an
isolated test and is part of the full chain test in `WorkoutMigrationTest`. A migration cannot be
undone; going back to an older app version means export, reinstall and import, and an older build
refuses a newer export version.

## Deletion

- **Delete all data** (Data screen, danger zone): after typing "delete", removes every workout, run,
  route, template, custom exercise, photo and setting. It turns off automatic backup first, so empty
  backups cannot replace the good ones, and leaves existing backup files alone. The starter catalog
  comes back. It is not available while a workout or run is live.
- Single items: a workout, run, route or template can be deleted from its screen. Catalog exercises
  are archived, never deleted, because past workouts refer to them.
- Uninstalling the app removes everything it stores. Backup files and Health Connect data stay.
