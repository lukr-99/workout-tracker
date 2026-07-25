# Phase 3 Report — Data services, import/export, and composable stats

> **Branch:** `codex/phase-3-data-services`  
> **Worktree:** `F:\Code\workout-tracker-phase3`  
> **Date:** 2026-07-25  
> **Baseline:** isolated snapshot of the in-progress Phase 1 data core (`2efaeda`)

## Result

Phase 3 is implemented as a complete data-service vertical slice. It imports the captured Lyfta
history and MAUI/Workout JSON, exports selective JSON and spreadsheet CSV, previews and resolves
conflicts before an atomic Room commit, creates validated domain graphs, and produces composable
multi-dimensional statistics. Android SAF/open/save/share endpoints and a standalone Compose data
screen are ready for Phase 2 to route from Settings.

No Phase 2 logging/catalog/history screen was changed, so Claude can continue that branch without
UI file collisions.

## Shipped

### Rich creation boundary

- Draft models for exercises, templates, sessions, entries, strength sets, and cardio.
- Configurable creation policy, structured validation issues, normalization, catalog snapshots,
  generated IDs, injected clock/ID providers, set/cardio graph parenting, warm-up normalization.
- `WorkoutDataService` persists only valid results and also exposes full snapshots and queries.

### Composable processing and stats

- `WorkoutFilter` supports nested `And`, `Or`, and `Not` across session/date/status/template,
  exercise/name/body-part/category, superset, set type, warm-up/PR, reps/weight, timed work, and
  cardio criteria.
- One query model drives stats and export selection.
- `StatsEngine` groups by any combination of exercise/body part/category/session/status/set type
  and day/week/month/year.
- Metrics include workouts, entries, sets, reps, volume, average/max weight, best Epley e1RM,
  duration, timed work, distance, calories, PR sets, and average RPE.
- Provider interfaces make new metrics/dimensions additive without changing request/response types.

### Import

- Dependency-free RFC-4180 CSV reader (quotes, escaped quotes, embedded newlines, BOM, delimiter
  detection, header normalization).
- Lyfta mapping: column aliases/order, local timezone → UTC, kg/lb normalization, aliases/fuzzy
  catalog resolution, custom exercise creation, all six set types, warmups, timed sets, cardio,
  supersets, session duration, and historical source-name snapshots.
- Workout/MAUI JSON 1.0 + 1.1 adapter with forward-field tolerance.
- Format registry via `TextDataImporter`.
- Dry preview with issues and insert/change/skip counts.
- Independent `Skip` / `Merge` / `Replace` / `KeepBoth` policies for session and catalog conflicts.
- Stable SHA-256 session fingerprints and graph-safe ID regeneration for keep-both.
- Single Room transaction for the accepted plan; repeat imports are idempotent.

### Export and Android transport

- Selective JSON backup using the existing portable `ExportBundle`.
- Configurable CSV columns/order, delimiter, line endings, BOM, timezone, kg/lb, and query.
- `ExportArtifact` keeps formatting separate from transport.
- SAF open/create document, bounded UTF-8 reads, document writes, scoped `FileProvider` sharing.
- `DataTransferViewModel` state machine and standalone `DataTransferScreen` for pick → preview →
  commit, save JSON/CSV, and share JSON/CSV.

## Verification

- `gradlew testDebugUnitTest` — **24/24 green**.
- `gradlew connectedDebugAndroidTest` — **11/11 green** on Galaxy A56, including Lyfta preview,
  transactional commit, and idempotent re-import through Room.
- Full ignored personal Lyfta export — **468 rows → 19 sessions**, no parser errors. Its 42 raw
  exercise labels contain one trailing-space variant, normalized to 41 logical names while the
  historical source label remains snapshotted.
- `git diff --check` — clean.

The only recurring build note is the pre-existing AGP 8.5.2 / compileSdk 35 warning. This machine
currently exposes Android Build Tools 36, so the app explicitly selects `36.0.0`; builds and device
tests are green.

## Main files

- `domain/creation/WorkoutFactory.kt`
- `domain/query/WorkoutQuery.kt`
- `domain/stats/StatsEngine.kt`
- `data/services/WorkoutDataService.kt`
- `data/transfer/{TransferModels,ImportPlanner,DataTransferService,AndroidDocumentGateway}.kt`
- `data/importer/{CsvReader,LyftaCsvImporter,BundleImporter}.kt`
- `data/export/CsvExporter.kt`
- `ui/{DataTransferViewModel,screens/DataTransferScreen}.kt`
- `docs/rework/07-data-services.md`

## Phase 2 merge contract

Claude should continue owning the logging/catalog/template/history UI. After the branches meet:

1. route Settings → `DataTransferScreen`;
2. create its VM from `AppContainer.dataTransfer` + `AppContainer.documents`;
3. use `AppContainer.workoutData` for new create/stats actions;
4. retain Phase 3 import/export/query implementations during conflict resolution.

No Room schema version change was needed.
