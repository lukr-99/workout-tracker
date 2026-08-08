# Handoff Prompt — Run Mode R2: Run detail, history & stats

> Paste the block below as the opening prompt for the agent. Self-contained. Builds on R0+R1 (already
> merged on `feature/run-mode`). Touches `ui/run/`, `domain/run/`, `data/run/`, `data/health/`, and the
> Progress tab.

---

You are building **R2 — Run detail, history & stats** for **Run Mode (v2.1)** of the app (native
Android, Kotlin/Compose/Room/MVVM, minimal-dark + ember; the strength app shipped as v2.0.0). Work on
branch **`feature/run-mode`** in `F:\Code\workout-tracker` (already checked out; commit + push there).

**Read first — decisions are LOCKED, follow them:** `docs/run-mode/README.md`, `architecture.md`,
`phases.md`, and the two prior reports `phase-R0-report.md` + `phase-R1-report.md` (they describe
exactly what already exists). Everything here is **additive** — do not regress the strength features or
the R1 live-run flow.

## What already exists (reuse it — do not rebuild)

- **Data:** Room **v5** with `runs`, `run_points`, `routes`, `route_points` (+ `5.json`, migration
  test). `data/run/RunRepository` is the only run IO boundary: `observeRuns()`, `getRun(id)` (loads the
  full trace), `saveRun`, `deleteRun` (cascades points), `exportRuns/exportRoutes`, and a Google
  polyline codec. `AppContainer.runRepository`.
- **Domain (pure, unit-tested):** `domain/run/RunModels` (`Run`, `Route`, `TracePoint`, `Split`,
  `RunSource`), `domain/run/Pace` (haversine, trace distance, speed, pace sec/km + sec/mi, elevation
  gain with threshold, **interpolated km/mi splits**, `formatPace`, `formatDuration`), and
  `domain/run/RunTracker` (the live reducer). `Run` carries `encodedPolyline`, `distanceMeters`,
  `movingSeconds`, `durationSeconds`, `avgPaceSecPerKm`, `elevationGainM`, `startedAtUtc`, optional
  `sessionId`, `routeId`, `notes`.
- **UI:** `ui/run/RunsScreen` (hub — lists saved runs with distance/time/pace; **`onOpenRun` is
  currently a no-op stub**), `ui/run/LiveRunScreen` + `LiveRunViewModel`, and the provider-agnostic
  `ui/run/components/MapView.kt` (`RunMap`) which already renders a **trace polyline** (`tracePoints:
  List<Pair<Double,Double>>`, `traceColor`) and follows location. `data/map/MapStyle` holds the dark
  style URL.
- **Existing strength UI to mirror/reuse:** `ui/components/Charts.kt` (`ProgressChart`/line charts),
  `VolumeBars`, `StatTile`, `Format` (has `Format.distance(meters, units)`, `Format.date`), the
  `ProgressHubScreen` (`Progress | History` segmented hub — add a Running section/section-switch here),
  and `WorkoutDetailScreen` as a screen-layout reference.
- **Health Connect:** `data/health/HealthConnectService` + `AndroidHealthConnectGateway` +
  `HealthConnectMapper` already write strength sessions idempotently; extend this, don't fork it.
- **Test harness (use it — see the testing rule below):** `tools/run-sim.ps1` replays a synthetic run
  through the live controller on-device; `RunSessionControllerTest` is an instrumented E2E of the
  record→save path. `WorkoutMigrationTest` covers migrations.

## Scope (R2)

1. **`ui/run/RunDetailScreen.kt`** (new, one-file-per-screen) + a `RunDetailViewModel` (or extend
   `RunViewModel`): open it from the Runs hub (**wire `RunsScreen.onOpenRun` → push a `RunDetail(runId)`
   route in `ui/App.kt`/`Navigation.kt`**). Show:
   - the full **map trace** (reuse `RunMap` with the loaded `run.trace` as `tracePoints`; fit the camera
     to the polyline bounds — add a `fitToTrace`/bounds option to `RunMap` if needed, provider-agnostic),
   - hero stats (distance, moving time, avg pace, elevation),
   - a **split table** (`ui/run/components/SplitTable.kt`) from `Pace.splits(run.trace, unit)`,
   - **pace + elevation charts** (`ui/run/components/PaceChart.kt`, `ElevationChart.kt` — mirror
     `ui/components/Charts.kt` style),
   - **edit** (notes/title) and **delete** (via `RunRepository.deleteRun`, with confirm).
2. **`domain/run/RunStats.kt`** (new, **pure Kotlin, unit-tested** — this is the bulk of the testable
   work): weekly/monthly distance, average-pace trend, **PRs** (fastest 1k/5k/10k/half-marathon,
   longest run, most elevation, best pace), and consistency/streak. Operate on `List<Run>` (+ traces
   where a PR needs sub-run windows). Mirror the strength `RecordsEngine`/`StatsEngine` style.
3. **Progress running section:** surface `RunStats` in the **Progress** tab (extend `ProgressHubScreen`
   / `ProgressViewModel` or add a Running sub-view) reusing `ProgressChart`/`VolumeBars`/`StatTile`.
   Respect the user's unit setting (km/mi) throughout.
4. **Health Connect write:** on run save (and/or a backfill action), write each run as an
   `ExerciseSessionRecord` (type Running) with an `ExerciseRoute` + distance/energy, **idempotent by a
   stable run fingerprint** — extend `data/health`. Optionally link a run to a **Cardio
   `WorkoutSession`** (`sessionId`) so runs appear in unified history/volume without duplicating logic;
   if you do, keep it non-destructive and behind the existing session model.
5. **Fold in the R1 loose ends:** hub run-row → detail navigation (above); optional live split readout
   is fine to defer; make sure delete updates the hub reactively (it will, via `observeRuns()`).

## Constraints

- `domain/run/**` stays **Android-free** and **unit-tested** (RunStats + any split/PR math). The
  **repository is the only Room/file boundary**; migrations (if any) additive + non-destructive with a
  checked-in `N.json` and extended `WorkoutMigrationTest`. R2 should need **no new tables** — the v5
  schema already has everything (add a column only if truly required, via a `MIGRATION_5_6` + `6.json`).
- One-file-per-screen Compose; match the design system (near-black, ember accent, the existing card/ча
  card/chart components). Keep the map behind `MapView` (provider-agnostic).
- Don't regress strength or the R1 live flow. New deps isolated; no secrets in the repo.
- **Bump `ExportBundle`** only if you add fields; it's already at 1.5 with `runs`/`routes`.

## Testing rule (important — the owner wants this)

Prefer **automatic, repeatable tests over vision/screenshots** (see
`memory/feedback-prefer-automated-tests.md`). Concretely:
- Unit-test all of `RunStats` (PRs, weekly/monthly, trends) and any new `domain/run` math.
- Add instrumented coverage for new repository/Health-Connect paths (mirror `RunSessionControllerTest`
  and the existing `data/health` instrumented tests).
- For on-device checks, seed data with **`tools/run-sim.ps1`** (e.g. run it a few times with different
  `-Meters/-Seconds/-Bearing` to populate varied runs) instead of driving the UI by hand. Only
  screenshot the final detail/Progress screens as evidence.

## Build / verify (current SDK caveat)

The canonical LOCALAPPDATA SDK is partly on a detached `E:` drive, so the repo is pinned to
`buildToolsVersion = "35.0.0"` but this machine builds against
`C:\Program Files (x86)\Android\android-sdk` (build-tools **36.0.0**). **Locally** set
`buildToolsVersion = "36.0.0"` and point `local.properties` `sdk.dir` there — **keep both changes
uncommitted** (the repo stays on 35.0.0). Then: `.\gradlew.bat testDebugUnitTest`, `assembleDebug`,
`connectedDebugAndroidTest` (filter with `-Pandroid.testInstrumentationRunnerArguments.class=…`), and
`.\tools\build-and-install.ps1` on the Galaxy A56. Stale-daemon "SDK location not found" →
`.\gradlew.bat --stop`. The debug APK triggers a One-UI "16 KB page-size" warning (MapLibre `.so`) —
non-blocking; release 16 KB alignment is an R5 concern.

## Done when

Past runs open from the hub into a **detail screen** with the map trace, **split table**, and **pace +
elevation charts**, and can be edited/deleted; **Progress** shows a Running section with distance/pace
trends and **PRs**; a saved run appears in **Health Connect** (idempotent); `RunStats` + new math are
**unit-tested** and green; strength + R1 flows are unaffected. Seed via `tools/run-sim.ps1`, verify on
the A56, and write `docs/run-mode/phase-R2-report.md`. Then R3 (route planning) builds on this.
