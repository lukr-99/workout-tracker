# Phase 3.5 Report — Records, recovery, progression, and catalog sync

> **Branch:** `codex/phase-3.5-analytics`
> **Base:** `feature/app-rework` at `4c6071d` (Phase 3 merged)
> **Scope:** non-UI domain/data services only

## Outcome

Phase 3.5 provides stable service APIs for the Phase 2 UI without adding or changing any screen.
The records, recovery, body-part statistics, and progression calculations are pure Kotlin. Network
transport and persistence remain behind `data/` services.

## Phase 2 entry point

`AppContainer` now exposes two additive lazy services:

```kotlin
val insights: WorkoutInsightsService by lazy { WorkoutInsightsService(repository) }
val wgerSync: WgerSyncService by lazy { WgerSyncService(repository) }
```

### Records

```kotlin
suspend fun records(
    exerciseId: String,
    repRange: IntRange = 1..12,
): ExerciseRecords

suspend fun evaluateSetRecord(
    exerciseId: String,
    candidate: StrengthSet,
    repRange: IntRange = 1..12,
): RecordAchievements

suspend fun evaluateSessionRecords(
    candidate: WorkoutSession,
    exerciseId: String,
    repRange: IntRange = 1..12,
): RecordAchievements
```

Example:

```kotlin
val records = container.insights.records(exerciseId)
val livePr = container.insights.evaluateSetRecord(exerciseId, enteredSet)
if (RecordKind.RepMax in livePr.kinds) {
    // render the PR treatment for livePr.repMaxReps
}
```

`ExerciseRecords` includes the heaviest set, best Epley e1RM, best set volume, best session volume,
and a 1–12 rep-max table. Every result carries `RecordSource` with session, entry, set, and date.
Only completed sessions and non-warmup working sets participate. Ties resolve to the oldest source,
then stable IDs, and are not announced as new PRs.

The same calculations are independently available as pure functions:

```kotlin
RecordsEngine.forExercise(sessions, exerciseId, repRange)
RecordsEngine.evaluateSet(sessions, exerciseId, candidate, repRange)
RecordsEngine.evaluateSession(sessions, candidateSession, exerciseId, repRange)
```

### Recovery and body-part statistics

```kotlin
suspend fun recovery(
    nowUtcMillis: Long = System.currentTimeMillis(),
    config: RecoveryConfig = RecoveryConfig(),
): RecoverySnapshot

suspend fun bodyPartStats(
    request: StatsRequest = defaultBodyPartStatsRequest(),
): StatsReport
```

Example:

```kotlin
val recovery = container.insights.recovery(
    config = RecoveryConfig(halfLifeHours = 36.0),
)
val chest = recovery.forBodyPart("Chest")

val weekly = container.insights.bodyPartStats()
```

`RecoveryConfig` tunes lookback windows, half-life, primary/secondary contribution, set/volume/cardio
load conversion, fatigue saturation, and the ready threshold. `RecoverySnapshot` provides each
muscle's 0–100 readiness, current fatigue, last-trained time, estimated ready time, and weekly
volume/set/load values.

The stats extension keeps the Phase 3 contract unchanged:

```kotlin
BodyPartStatsKeys.AllBodyParts       // "body_part_all"
BodyPartStatsKeys.WorkingSetCount    // "working_set_count"
BodyPartStatsKeys.WorkingVolumeKg    // "working_volume_kg"
BodyPartStatsProviders.metrics
BodyPartStatsProviders.bodyPartDimension(exercises)
```

`ExpandingDimensionProvider` is an additive `DimensionProvider` subtype. It lets one set contribute
to both its primary and secondary body-part groups while all existing single-value dimensions keep
their original behavior.

### Progression

```kotlin
suspend fun progression(
    exerciseId: String,
    scheme: ProgressionScheme,
    deload: DeloadPolicy = DeloadPolicy(),
): ProgressionSuggestion
```

Example:

```kotlin
val next = container.insights.progression(
    exerciseId,
    DoubleProgression(repRange = 8..12, weightIncrementKg = 2.5),
    DeloadPolicy(stallSessions = 3),
)
```

Supported schemes are `DoubleProgression`, `LinearProgression`, and
`PercentOfEstimated1Rm`. The immutable result contains target sets, a rationale, contributing
session IDs, current e1RM, and whether the load/volume targets are a deload. The pure entry point is:

```kotlin
ProgressionSuggestionEngine.suggest(sessions, exerciseId, scheme, deload)
```

### wger catalog sync

```kotlin
suspend fun WgerSyncService.sync(
    options: WgerSyncOptions = WgerSyncOptions(),
): WgerSyncSummary
```

Example:

```kotlin
val result = container.wgerSync.sync(
    WgerSyncOptions(language = 2, limit = 500, pageSize = 50),
)
// result.added / updated / skipped / pages / warnings
```

The client uses bounded paging, tolerant JSON decoding, cancellation-aware OkHttp calls, and
same-origin validation for server-provided next links. `WgerPageSource` and
`ExternalExerciseMerger` are replaceable seams for tests or future catalog providers.

Repository merging is deliberately conservative:

```kotlin
suspend fun mergeExternalExercisesDetailed(
    exercises: List<Exercise>,
): ExternalExerciseMergeSummary
```

- New external IDs are inserted as `ExerciseSource.Synced`.
- Only existing `Synced` rows can be enriched.
- Existing names, notes, archive state, category, and defaults are retained.
- Blank equipment/body-part fields may be filled and secondary body parts may be appended.
- Custom/seed external-ID matches, name collisions, duplicate input IDs, and unchanged rows skip.
- The older `mergeExternalExercises(...): Int` remains compatible and returns added + updated.

The manifest includes `INTERNET`. OkHttp is isolated to `data/sync`; no network or Android type
crosses into `domain/`.

## Migration safety

`WorkoutMigrationTest` creates a database from the checked-in Room schema-1 asset, inserts a fixture,
and reopens it through `WorkoutDb` so Room validates the baseline. Future schema versions can add
migrations and version-to-version cases to this harness. Phase 3.5 makes no schema change.

## Verification

- `testDebugUnitTest`: records, progression, recovery/body-part expansion, Wger paging/mapping, and
  all prior Phase 1/3 JVM tests.
- `connectedDebugAndroidTest`: repository safety, external merge behavior, schema-1 migration
  scaffold, and all prior Room/import tests on the connected Galaxy A56.
- Debug APK assembled and installed through ADB after the suites passed.
- No file under `app/src/main/java/com/lukr99/workout/ui/` was changed.
