# Target Architecture

Native Android, single app module, layered **MVVM** — the same shape as the ring-set app,
adapted to a workout domain. Everything runs on-device; there is no backend.

```
┌──────────────┐        ┌──────────────┐      ┌───────────────┐
│    data/     │──────▶ │  domain/     │────▶ │   ui/ (MVVM)  │
│ Room + DAOs  │  Flow  │ pure logic   │      │ ViewModel +   │
│ Repository   │◀────── │ (analytics,  │◀──── │ Compose UI    │
│ import/export│        │  progression)│      │               │
└──────────────┘        └──────────────┘      └───────────────┘
        ▲                                              │
        └─────────── SharedPreferences / DataStore ────┘
                     (settings, theme, units)
```

## Tech choices

| Concern | Choice | Notes |
|--------|--------|-------|
| Language | **Kotlin** | Matches ring-set; modern Android default. |
| UI | **Jetpack Compose + Material 3** | Declarative, animation-friendly, custom charts. |
| Architecture | **MVVM**, unidirectional data flow | `AndroidViewModel` exposes `StateFlow`; screens `collectAsStateWithLifecycle`. |
| Persistence | **Room** (SQLite) | Reactive `Flow` queries; typed entities; migrations. |
| Settings | **DataStore (Preferences)** | Theme, unit system, rest-timer defaults. |
| Navigation | **Compose Navigation** or an in-app `Screen` enum | Start with the ring-set floating-nav enum pattern; adopt Nav-Compose when we need deep links/back-stack per tab. |
| DI | Manual (a `ServiceLocator`/`AppContainer`) → **Hilt** if it grows | ring-set uses manual wiring; fine until the graph gets deep. |
| Charts | **Custom Compose Canvas** | Mirrors ring-set's `MetricChart`; no heavy chart lib, full visual control. |
| Async | Coroutines + Flow | Room + `viewModelScope`. |
| Serialization | **kotlinx.serialization** | JSON export/import; stable versioned schema. |
| Build | Gradle (Kotlin DSL), version catalog | `gradle/libs.versions.toml`. |
| Min SDK | 26 (Android 8) | Same era as MAUI's 21 floor but modern enough for Compose comfort; revisit. |

## Package structure

Package root: `com.lukr99.workout` (keeps continuity with the MAUI `com.lukr99.workouttracker`
application id; the rework can ship under a new id or reuse it — decided at release time).

```
com.lukr99.workout
├─ MainActivity.kt            Compose host, permissions, edge-to-edge, share/open-document intents.
│
├─ data/
│  ├─ Entities.kt             Room @Entity: exercises, templates, template_exercises,
│  │                          sessions, entries, strength_sets, cardio_data.
│  ├─ WorkoutDao.kt           Queries (Flow-returning) + upserts + analytics projections.
│  ├─ WorkoutDb.kt            RoomDatabase + migrations + type converters.
│  ├─ WorkoutRepository.kt    Single API over the store; owns import/export orchestration.
│  ├─ Models.kt               In-flight / projection models (summaries, snapshots, points).
│  ├─ Seed.kt                 Seeded starter exercise catalog (ported from SeedExercises.cs).
│  ├─ export/
│  │  ├─ ExportBundle.kt      Versioned @Serializable bundle (the cross-device contract).
│  │  ├─ JsonExporter.kt      Bundle <-> JSON.
│  │  └─ CsvExporter.kt       Flat CSV for spreadsheets.
│  └─ importer/
│     ├─ BundleImporter.kt    Our own JSON bundle (round-trip, restore).
│     └─ LyftaCsvImporter.kt  Maps Lyfta CSV -> our domain (see 05-lyfta-import.md).
│
├─ domain/                    Pure Kotlin — no Android imports, unit-testable in isolation:
│  ├─ Analytics.kt            Overview, consistency, streaks, volume, PRs.
│  ├─ Progression.kt          Per-exercise best set / e1RM / volume-over-time series.
│  ├─ Estimates.kt            Estimated 1RM (Epley/Brzycki), volume, tonnage.
│  └─ Units.kt                kg/lb + distance conversions, formatting helpers.
│
├─ settings/
│  └─ SettingsStore.kt        DataStore: theme mode, unit system, timer defaults.
│
└─ ui/
   ├─ App.kt                  Top-level scaffold: Screen enum + floating nav + routing.
   ├─ WorkoutViewModel.kt     (or split VMs per area) exposes repo/domain state + actions.
   ├─ theme/
   │  ├─ Theme.kt             Material 3 dark/light schemes (design-system.md palette).
   │  ├─ Color.kt             Named tokens + per-body-part accent map.
   │  └─ Type.kt              Type scale.
   ├─ components/             Reusable, screen-agnostic:
   │  ├─ ScreenHeader.kt      Title + subtitle + info affordance (ring-set pattern).
   │  ├─ StatTile.kt          Compact metric tile / KPI.
   │  ├─ SetRow.kt            Editable reps × kg row with steppers + RIR/RPE.
   │  ├─ NumberStepper.kt     Reusable editable numeric input.
   │  ├─ ProgressChart.kt     Canvas line/area chart with scrub (from ring-set MetricChart).
   │  ├─ VolumeBars.kt        Weekly volume / frequency bars.
   │  ├─ BodyHeatmap.kt       Body-part distribution (optional muscle map).
   │  ├─ RestTimerBar.kt      Sticky live rest-timer surface.
   │  ├─ Chips.kt             Filter chips, choice chips, body-part tags.
   │  └─ Dialogs.kt           Confirm / picker scaffolding.
   └─ screens/                One file per tab/flow, each `fun XScreen(vm)`:
      HomeScreen · WorkoutScreen (live) · TemplatesScreen · TemplateEditorScreen ·
      CatalogScreen · ExerciseEditorScreen · HistoryScreen · WorkoutDetailScreen ·
      StatsScreen · SettingsScreen
```

## Conventions (inherited from ring-set)

- **UI is declarative and mostly stateless.** Screen state lives in the ViewModel/Room;
  transient view state in `remember`. Each screen is `fun XScreen(vm: WorkoutViewModel)`.
- **Reusable visuals go in `ui/components/`.** Anything used by a single screen stays private
  in that screen file until a second caller appears.
- **No Android types in `domain/`.** It takes primitives/models and returns models. This is
  also what keeps the analytics portable toward a future desktop tool.
- **The repository is the only thing that touches Room and files.** ViewModels call the repo;
  screens call the ViewModel.
- **History is immutable-by-snapshot.** Completed sessions store snapshot exercise fields so
  later catalog edits never rewrite the past (carried over from the MAUI data rules).

## Portability seam (phone ↔ future Windows tool)

We are **not** doing Kotlin Multiplatform. Instead the contract between devices is the
**versioned `ExportBundle` JSON** (`data/export/ExportBundle.kt`):

- `domain/` stays pure Kotlin so its logic *could* be lifted into a KMP module later without a
  rewrite, but that's an option, not a commitment.
- Any Windows/desktop tool (built in any language) consumes/produces the same JSON bundle.
- The bundle carries `exportFormatVersion`; importers must tolerate older versions.
- Transport is manual for now (share sheet / file / USB pull via `tools/`). A sync service is a
  much later, optional milestone — see [04-feature-roadmap.md](04-feature-roadmap.md).

## Testing

- `domain/` — pure JVM unit tests (analytics, estimates, progression, unit conversions).
- `data/` — Room instrumented tests for DAOs, migrations, and import/export round-trips
  (mirrors the MAUI `WorkoutTracker.Tests` repository coverage).
- Import fixtures: a checked-in sample Lyfta CSV and a sample `ExportBundle` JSON.
