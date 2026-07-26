# Phase 4 services report — Health Connect, automatic backup, and export v1.2

## AppContainer entry points

Phase 4 adds two lazy, additive services. No UI package is changed.

```kotlin
val healthConnect: HealthConnectService
val backup: BackupScheduler
```

## Health Connect

`AppContainer.healthConnect` exposes:

```kotlin
val requiredPermissions: Set<String>
suspend fun availability(): HealthConnectAvailability
suspend fun hasPermissions(): Boolean
suspend fun exportCompletedSessions(): HealthConnectSyncSummary
suspend fun importSessions(
    fromUtcMillis: Long = /* 30 days ago */,
    toUtcMillis: Long = System.currentTimeMillis(),
): HealthConnectSyncSummary
```

`HealthConnectAvailability` is `Available`, `ProviderUpdateRequired`, or `Unavailable`.
`HealthConnectSyncSummary` reports `imported`, `exported`, `skipped`, and `unsupported`.

The UI must request all strings in `requiredPermissions`:

- `android.permission.health.READ_EXERCISE`
- `android.permission.health.WRITE_EXERCISE`
- `android.permission.health.READ_WEIGHT`
- `android.permission.health.WRITE_WEIGHT`

Example:

```kotlin
if (container.healthConnect.availability() == HealthConnectAvailability.Available &&
    container.healthConnect.hasPermissions()
) {
    val result = container.healthConnect.importSessions()
    // Render result.imported / skipped.
}
```

Completed local sessions are written as `ExerciseSessionRecord`s, with a `WeightRecord` at the
session start when log-time bodyweight is present. Their Health Connect
`clientRecordId` is `workout-tracker:` plus the existing stable SHA-256 session fingerprint, so a
retry deduplicates instead of creating another record. Imported sessions are created through
`WorkoutFactory`, persist `source = HealthConnect` and a provider `externalKey`, and are
deduplicated on future reads. Health Connect SDK types stay in `data/health`.

The manifest declares exercise and weight permissions, the provider package query, Android 13
rationale activity, and Android 14 permission-usage activity alias. The rationale activity is
intentionally a non-rendering stub; the UI branch must route it to the final privacy-policy content
before release.

The project is pinned to compileSdk 35 / AGP 8.5.2, so it uses Health Connect
`connect-client:1.1.0-alpha11`, the newest 1.1 artifact whose AAR metadata supports that toolchain.

## Automatic SAF backup

`AppContainer.backup` exposes:

```kotlin
val state: Flow<BackupState>

suspend fun enable(
    treeUri: Uri,
    intervalHours: Long = 24,
    retentionCount: Int = 7,
)

suspend fun disable()
```

`BackupState` contains `enabled`, the persisted tree URI, interval, retention, last-run timestamp,
`lastResult`, and an optional failure message.

Example after `ACTION_OPEN_DOCUMENT_TREE`:

```kotlin
container.backup.enable(
    treeUri = selectedTreeUri,
    intervalHours = 24,
    retentionCount = 7,
)
```

`enable` persists read/write access to the chosen SAF tree and enqueues unique periodic work named
`automatic-workout-backup`. `BackupWorker` exports normal v1.2 JSON through
`DataTransferService`, writes `workout-backup-yyyyMMdd-HHmmss-SSS.json`, and retains only the newest
N managed backup files. Unrelated files in the selected tree are never deleted. Periodic execution
is inexact and battery-aware, as required by WorkManager.

## Export v1.2 and database migration

`ExportBundle.CURRENT_VERSION` is `1.2`; readers accept `1.0`, `1.1`, and `1.2` and continue to
ignore unknown fields. `WorkoutSession` adds:

```kotlin
val source: WorkoutSessionSource = WorkoutSessionSource.Local
val externalKey: String? = null
```

Room schema 2 persists these fields. `MIGRATION_1_2` adds the defaulted `source`, nullable
`externalKey`, and its lookup index without rewriting existing history. Older exports deserialize
to `Local` / `null`.

## Additive statistics

`MetricKeys.SmoothedE1rmKg` (`"smoothed_e1rm_kg"`) is registered in `BuiltInMetrics`. It takes the
best non-warmup Epley estimate per session and applies a chronological EWMA with alpha `0.35`.

Example:

```kotlin
container.workoutData.calculateStats(
    StatsRequest(
        dimensions = listOf(DimensionKeys.Week, DimensionKeys.ExerciseId),
        metrics = listOf(MetricKeys.SmoothedE1rmKg),
    ),
)
```

Weekly working-set count per muscle was not duplicated: Phase 3.5 already provides
`BodyPartStatsKeys.WorkingSetCount` with the expanding all-body-parts dimension.

## Verification

- `testDebugUnitTest`: 40 tests, all passed.
- `connectedDebugAndroidTest`: 17 tests, all passed on Galaxy A56 (`SM-A566B`, Android 16).
- `lintDebug`: passed (existing dependency-update/toolchain warnings only).
- Debug APK installed and cold-launched through ADB; all four Health Connect permissions are
  declared and surfaced by the installed package.
- No file under `app/src/main/java/com/lukr99/workout/ui/` was changed.
