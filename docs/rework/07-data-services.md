# Phase 3 Data Services

Phase 3 turns the Phase 1 repository into a composable data platform. The app remains local-first:
these are in-process Kotlin service APIs, not HTTP endpoints.

## Entry points

`AppContainer` owns four stable entry points:

| API | Purpose |
|---|---|
| `repository` | Reactive Room persistence and the Phase 1 behavior surface. |
| `workoutData` | Validated exercise/template/session creation, snapshots, queries, and stats. |
| `dataTransfer` | Format detection, import preview/planning/commit, JSON backup, and CSV export. |
| `documents` | Android Storage Access Framework and share-sheet file transport. |

`DataTransferViewModel` and the standalone `DataTransferScreen` implement the Android-facing
pick → preview → commit and save/share state machine. Phase 2 only needs to route to the screen.

## Creation

`domain/creation/WorkoutFactory` accepts partial draft models and a `CreationPolicy`. It produces a
`CreationResult<T>` with normalized data plus structured issues. It has injected ID and time
providers, so UI actions, import adapters, sync jobs, and tests can use identical rules.

```kotlin
val result = container.workoutData.createSession(
    SessionDraft(
        name = "Push",
        entries = listOf(
            EntryDraft(
                exerciseId = benchId,
                strengthSets = listOf(StrengthSetDraft(reps = 5, weightKg = 100.0)),
            ),
        ),
    ),
)
if (!result.isValid) show(result.issues)
```

## Composable queries and statistics

`WorkoutFilter` is an `All` / `And` / `Or` / `Not` tree over optional criteria. The same
`WorkoutQuery` selects data for stats, exports, dashboards, and future sync.

```kotlin
val filter =
    WorkoutCriterion.StartedBetween(fromUtc, toUtc).asFilter() and
    WorkoutCriterion.BodyParts(setOf("Chest")).asFilter() and
    !WorkoutCriterion.Warmup(true).asFilter()
```

`StatsRequest` combines any number of dimensions and metrics:

```kotlin
StatsRequest(
    query = WorkoutQuery(filter),
    dimensions = listOf(DimensionKeys.Week, DimensionKeys.Exercise),
    metrics = listOf(MetricKeys.Workouts, MetricKeys.VolumeKg, MetricKeys.BestE1rmKg),
)
```

Built-in dimensions cover exercise, body part, category, session, status, set type, day, ISO week,
month, and year. Built-in metrics cover workouts, entries, sets, reps, volume, average/max weight,
e1RM, session/timed duration, cardio distance, calories, PR sets, and average RPE.

New metrics and dimensions are additive: implement `MetricProvider` or `DimensionProvider` and pass
it to `StatsEngine`. Requests use stable string keys, so adding a provider does not change the
request/response data classes.

## Import pipeline

Every import follows the same side-effect-free pipeline:

1. Detect a `TextDataImporter` by confidence, or use `ImportOptions.formatHint`.
2. Parse into `ImportedPayload` without writing.
3. Plan catalog/template/session actions against a current `ImportContext`.
4. Return an `ImportPreview` with counts, issues, date range, and every planned action.
5. Commit the accepted plan in one Room transaction.

`TextDataImporter` is the format extension point. Phase 3 includes:

- Workout JSON (`ExportBundle` 1.0 and 1.1; unknown fields tolerated).
- Lyfta CSV (RFC-4180 quotes, header aliases/order, local timezone → UTC, kg/lb, set types, timed
  sets, cardio, supersets, catalog matching, and source-name snapshots).

Conflict policies are independent for sessions and catalog data: `Skip`, `Merge`, `Replace`, and
`KeepBoth`. Exercise matching supports exact, normalized, aliases, fuzzy threshold, or always
create. Exact session dedupe uses a stable SHA-256 content fingerprint; date/title identity catches
changed versions of an existing session.

## Export pipeline

- JSON can select sessions with any `WorkoutQuery`, include/exclude discarded sessions, templates,
  and unreferenced catalog exercises.
- CSV is one row per strength set or cardio activity. Callers choose columns, order, delimiter,
  newline, UTF-8 BOM, timezone, kg/lb, filename, and the same composable query.

`ExportArtifact` is transport-neutral. `AndroidDocumentGateway` writes it to a SAF URI or exposes it
through a scoped `FileProvider` share URI. A future desktop/CLI adapter can write the same artifact
without importing Android types.

## Integration boundary with Phase 2

Phase 2 should consume these APIs rather than duplicating data rules in ViewModels:

- creation actions → `AppContainer.workoutData`;
- custom stats/query screens → `WorkoutDataService.calculateStats/querySessions`;
- Settings “Data” destination → `DataTransferViewModel` + `DataTransferScreen`;
- do not parse CSV, build export rows, or resolve import conflicts in UI code.

