# Ember architecture

## Context

Ember is a personal Android app for one owner. It logs strength workouts and records GPS runs.
Everything runs on the phone: there is no backend and no account. The Room database on the phone is
the source of truth. The versioned JSON export is the contract with anything outside the app:
backups, restores and any future desktop tool.

## Modules and dependencies

One Gradle module, `:app`, package `com.lukr99.workout`, split by role:

```text
ui/ (Compose screens, ViewModels) ---> domain/ (pure Kotlin)
   |                                       ^
   v                                       |
data/ (Room, files, network, Health Connect) --+
settings/ (DataStore)    update/ (GitHub release updater)
```

| Package | Responsibility | May depend on |
|---|---|---|
| `domain/` | Models and the export contract (`Models.kt`), analytics, records, recovery, progression, stats, queries, run maths, validated creation (`creation/WorkoutFactory`), snapshot copies (`WorkoutSnapshots.kt`) and live workout edits (`WorkoutDraftEdits.kt`). No Android imports. | nothing else in the app |
| `data/` | `WorkoutRepository` and `RunRepository` (the only Room users, with entity mapping in `WorkoutEntityMapping.kt`), numbered Room migrations in `data/migrations/`, import and export, backup, Health Connect, wger sync, images, location service, routing, map tiles, music. | `domain/`, `settings/` |
| `settings/` | `SettingsStore`: theme, units, default rest (Preferences DataStore). | nothing |
| `update/` | The updater chain: `GitHubReleaseSource`, `VersionPolicy`, `ArtifactSelector`, `VerifiedDownloader` (HTTPS, size, SHA-256), `PackageSignatureCheck`, `FileProviderInstallerLauncher`, joined by `UpdateService`. Only the Android adapters touch Android. | nothing else in the app |
| `ui/` | `App.kt` shell and custom `Navigator`, one ViewModel per area, screens in `ui/screens/` and `ui/run/`, shared pieces in `ui/components/`, tokens in `ui/theme/`. | everything above |

The composition root is `data/AppContainer.kt`, created once by `WorkoutApp`. ViewModels get their
dependencies through `factory(container)`. `WorkoutApp` also gives WorkManager its configuration,
and `BackupWorkerFactory` hands the backup to `BackupWorker`, so the default WorkManager initializer
is removed in the manifest.

Android creates two classes itself and offers them no constructor injection. They read the container
from `WorkoutApp`, and nothing else may:

- `MainActivity`, which hands the container to `App`.
- `LocationService`, the foreground service that records runs.

`LiveRunScreen` still builds its own `RunCues` (speech and vibration tied to the screen). The
redesign replaces that screen and moves it into the container.

## Data flow

- **Reads:** Room DAOs return `Flow`s. Repositories map Room entities to `domain/` models, and
  ViewModels expose `StateFlow`s that screens collect.
- **Live workout:** `LiveWorkoutViewModel` keeps an in-memory draft of the active session and
  saves it on structural changes, on set done, on leaving the screen, and on finish or discard.
  A live session survives process death. The edits themselves are pure functions in
  `domain/WorkoutDraftEdits.kt`, and `RestTimer` runs the rest countdown.
- **History is a snapshot:** an entry copies the exercise's name, category and body part when it is
  logged. Catalog exercises are archived, never deleted, so past workouts never change.
- **Runs:** `LocationService` is a foreground service that holds a partial wake lock while a run
  records. `RunSessionController` feeds fixes to the pure `domain/run/RunTracker`, and
  `RunRepository` stores runs, trace points, routes and route points.
- **Export and import:** `DataTransferService` writes the `ExportBundle` JSON (format 1.8), a full
  backup with settings and photos, and a flat CSV. It imports our own bundles and Lyfta CSV, and
  plans every import (preview, merge or replace, counts) before writing. A commit is one
  transaction, with photos staged to new files first (`PhotoArchive`, `SettingsArchive`). Single
  runs import and export as GPX from the Runs screen (`GpxCodec`). The full contract is in
  [docs/data-contract.md](docs/data-contract.md).
- **Delete all data:** `UserDataEraser` stops automatic backup, empties the database, photos and
  settings, and reseeds the starter catalog.
- **Automatic backup:** WorkManager runs `BackupWorker` daily or weekly. It writes the same JSON
  bundle into a folder the owner picks (Storage Access Framework) and keeps the newest N files
  (`workout-backup-*.json`).
- **Health Connect:** optional. Exports finished workouts and runs, and imports workouts as
  sessions.
- **Settings:** Preferences DataStore, included in every backup and restored by a replace-restore.

## Capability modules

| Capability | Interface or entry point | Adapter |
|---|---|---|
| Health Connect | `HealthConnectGateway` | `AndroidHealthConnectGateway` |
| Backup storage | `BackupGateway` | Storage Access Framework tree |
| Music | `SpotifyController` | `StubSpotifyController` (opens Spotify only) |
| Routing | `RoutingClient` | OSRM public server |
| Exercise catalog | `WgerSyncService` with `ExternalExerciseMerger` | wger REST API |
| Exercise images | `FreeExerciseImageIndex`, `ExercisePhotoStore` | free-exercise-db, app files |

## Connections

All network use is optional, and the app works fully offline. Every peer is plain HTTPS with no
authentication:

- GitHub Releases API for updates.
- `wger.de` for catalog sync.
- `router.project-osrm.org` for snapping planned routes to roads.
- OpenFreeMap tiles for the run map, with an offline tile cache.
- `raw.githubusercontent.com` for free-exercise-db images.

Failures show as a message on the screen that started the call. There are no background retries.

## Delivery

- **Release:** `com.lukr99.workout`, signed with the owner's keystore, R8 on, arm64 only. It is
  published as a GitHub Release with the APK attached.
- **Debug and test:** `com.lukr99.workout.debug`, "Ember dev", version `x.y.z-dev`. It installs
  beside the release app and never updates itself.
- **Updates:** Settings, Updates, Check. The updater compares the release tag with the installed
  version, downloads the APK and opens the system installer. Android enforces the signature match.
- **Manual path:** `tools/make-phone-installer.ps1` builds a USB installer zip.
- Details, keystore backup and first-install steps are in `docs/RELEASING.md`.

## Known constraints

- Room migrations are one numbered file each in `data/migrations/` (`Migration0008ExerciseGuides`),
  listed in order by `WorkoutMigrations.ALL`. Schemas 1 to 8 are checked in under `app/schemas/`.
- Five screen files are still over 400 lines: `LiveRunScreen`, `SettingsScreen`,
  `LiveWorkoutScreen`, `WorkoutDetailScreen` and `App.kt`. The planned redesign rewrites them, so
  they were not split first. Split any of them you change before then.
- Strings are written in the Compose code, not in `strings.xml`. The planned redesign moves them.
- `androidx.navigation.compose` is declared but unused; navigation is the custom `Navigator`.
