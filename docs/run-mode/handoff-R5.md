# Handoff Prompt — Run Mode R5: Polish (cues, GPX, offline, share) + deferred slices

> Paste the block below as the opening prompt for the agent. Self-contained. Builds on R0–R4 (all
> merged on `feature/run-mode`).

---

You are building **R5 — Polish** for **Run Mode (v2.1)** of the app (native Android,
Kotlin/Compose/Room/MVVM, minimal-dark + ember; the strength app shipped as v2.0.0). Work on branch
**`feature/run-mode`** in `F:\Code\workout-tracker` (already checked out; commit + push there).

**Read first — decisions are LOCKED, follow them:** `docs/run-mode/README.md`, `architecture.md`,
`phases.md`, and the phase reports `phase-R0-report.md` … `phase-R4-report.md` (they describe exactly
what exists). Everything here is **additive** — do not regress the strength features or the R0–R4 run
flow.

## What already exists (reuse — do not rebuild)

- **Data / Room v5:** `runs`, `run_points`, `routes`, `route_points` (+ `5.json`, `WorkoutMigrationTest`).
  `data/run/RunRepository` is the only run IO boundary: `observeRuns`/`getRuns`/`getRun` (with trace)/
  `getRunsWithTraces`/`saveRun`/`updateRunNotes`/`deleteRun`, `observeRoutes`/`getRoutes`/`getRoute`/
  `saveRoute`/`deleteRoute`, `exportRuns`/`exportRoutes`, and a Google **polyline codec**.
  `ExportBundle` is already at **1.5** (carries `runs` + `routes`).
- **Domain (pure, unit-tested):** `RunModels` (`Run`/`Route`/`TracePoint`/`RoutePoint`/`Split`/
  `RunSource`), `Pace` (haversine, distances, pace sec/km + sec/mi, **splits**, elevation gain,
  formatters, `pathDistanceMeters`), `RunTracker` (live reducer), `RunStats` (totals, weekly/monthly,
  pace trend, streak, **PRs** incl. `fastestTimeForDistanceMs`), `RouteDeviation` (distance-to-route).
- **Live run + service:** `data/location/RunSessionController` (process singleton; crash-buffer
  persistence; `armRoute` links a run to a saved route) + `LocationService` (foreground GPS +
  ongoing notification). `ui/run/LiveRunScreen`+`LiveRunViewModel` (record, 3-2-1 countdown,
  pause/resume, finish sheet, faint **planned-route underlay**).
- **Detail / stats / hub:** `RunDetailScreen` (map trace fit, split table, pace + elevation charts,
  edit notes, delete, linked-route name mention), `RunningProgressSection` (Progress tab), `RunsScreen`
  (recent runs + saved routes + start).
- **Routing (R3):** `data/routing/RoutingClient` + `OsrmRoutingClient` (keyless snap-to-roads);
  `RoutePlannerScreen` (tap → snap → save).
- **Music (R4):** `data/music/SpotifyController` + `StubSpotifyController` (**Open Spotify** only) behind
  the shared, small `ui/components/MusicMiniControls` on both live screens; `AppContainer.spotify`.
- **Map:** provider-agnostic `ui/run/components/MapView.kt` (`RunMap`) — trace polyline, faint planned
  route, waypoints, `fitTrace`, follow-location. `data/map/MapStyle` holds the dark style URL.
- **Health Connect:** `HealthConnectService.exportRuns` (session + `ExerciseRoute` + distance + energy,
  idempotent) fired best-effort after finish.
- **Test harness (use it — see the rule below):** JVM unit tests + instrumented `RunSessionControllerTest`
  / `WorkoutMigrationTest`; **debug** `RunSimReceiver` (seed a run) + `RunDevReceiver`
  (`DEV_SEED_ROUTE`/`DEV_CLEAR`/`DEV_DUMP`); `tools/run-sim.ps1`, `tools/run-mode-check.ps1`
  (build→seed→screenshot→assert, non-zero on fail), and `tools/phone.ps1` (`tap`/`swipe`/`key`/
  `seed-route`/`dump`).

## Scope (R5)

1. **Cues & finish summary** — audio/haptic **split cues** at each km/mi (respecting the unit setting),
   a start **countdown** cue (the visual countdown already exists in `LiveRunScreen`), and a **finish
   summary** (distance/time/pace/PRs) with an optional voice/haptic. Keep the audio layer thin and
   isolated (`data/…` or a small `ui` helper); make the trigger logic pure/testable (derive "a split
   was just crossed" from `RunTracker`/`Pace`, unit-tested).
2. **GPX import/export** — export a run's trace as **GPX 1.1** (`<trkpt>` with time/ele), and import a
   GPX into a `Run` (source `Imported`) via `RunRepository.saveRun`. Pure parser/serializer in
   `domain/run` (unit-tested against a fixture); the file IO/SAF glue reuses `data/transfer`.
3. **Offline tile caching** — cache the map region around a saved route / recent area so a run works
   without network. This is the one map-provider-specific piece; keep it behind `MapView`/`data/map`
   (MapLibre offline region APIs, or the locked Protomaps/PMTiles path). Add a small UX to trigger/
   show cached regions. Also finishes the deferred **R3** offline-for-a-route slice.
4. **Share-run card** — render a shareable image (map thumbnail + key stats) for a run and share via
   the existing SAF/`FileProvider` intent. No new external services.
5. **Reduce-motion & units respect** — honor the system reduce-motion setting for run animations
   (countdown, polyline), and make sure every new surface uses the km/mi + pace unit already threaded
   through (`Format`, `Pace.paceSecPerMile`).
6. **Fold in the deferred slices:**
   - **R3:** route **rename/delete** from the saved-routes list; an optional, **non-intrusive**
     live **off-route indicator** (use `RouteDeviation`; the planned route is a reference only — never
     force the runner onto it).
   - **R4 (user-gated):** if — and only if — the owner has added the Spotify **App Remote AAR** to
     `app/libs/` and a **client id** to a git-ignored `spotify.properties`, implement
     `AppRemoteSpotifyController` (connect + `PlayerState` → `available`/`track`, map play/pause/next/
     previous), wire one-time auth, and swap `AppContainer.spotify`. The UI already lights up unchanged.
     If those inputs are absent, **skip R4** and note it — do not stub a fake remote.

## Constraints

- `domain/run/**` stays **Android-free** and **unit-tested** (GPX codec, split-cue trigger logic, any
  new math). The **repository is the only Room/file boundary**. Any schema change is additive +
  non-destructive with a checked-in `N.json` and an extended `WorkoutMigrationTest` (R5 likely needs
  **no** new tables). One-file-per-screen Compose; match the design system; keep the map behind `MapView`.
- Don't regress strength or the R0–R4 run flow. New deps isolated; **no secrets in the repo** (Spotify
  client id + any tile key live in git-ignored properties).

## Testing rule (the owner wants this)

Prefer **automatic, repeatable tests over vision/screenshots** (see `memory/feedback-prefer-automated-
tests.md`). Unit-test the GPX codec and split-cue logic; extend instrumented coverage where a
repository/DB path changes. For on-device checks, **seed with `tools/run-sim.ps1` / `run-mode-check.ps1`**
(and `phone.ps1 seed-route`/`dump`) instead of hand-driving; only screenshot final surfaces as evidence.

## Build / verify (current SDK caveat)

The canonical LOCALAPPDATA SDK is partly on a detached `E:` drive, so the repo is pinned to
`buildToolsVersion = "35.0.0"`, but this machine builds against
`C:\Program Files (x86)\Android\android-sdk` (build-tools **36.0.0**). **Locally** set
`buildToolsVersion = "36.0.0"` + point `local.properties` `sdk.dir` there — **keep both uncommitted**.
Then `.\gradlew.bat testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest`
(`-Pandroid.testInstrumentationRunnerArguments.class=…`), and `.\tools\build-and-install.ps1` on the
Galaxy A56. Stale-daemon "SDK location not found" → `.\gradlew.bat --stop`. The debug APK's One-UI
"16 KB page-size" warning (MapLibre `.so`) is non-blocking; **16 KB release alignment is itself an R5
packaging item to close.**

## Done when

Split cues fire (audio/haptic, unit-aware) with a finish summary; **GPX round-trips** (export→import);
a run's map region can be **cached for offline**; a run can be **shared as an image**; reduce-motion +
units are respected; routes can be **renamed/deleted** and going off a planned route is flagged
non-intrusively; (Spotify transport done **iff** the AAR + client id were provided). New codecs/logic
are **unit-tested** and green; strength + prior run flows unaffected. Seed via the harness, verify on
the A56, and write `docs/run-mode/phase-R5-report.md`. Then Run Mode is ready to bump to **v2.1.0** and
merge `feature/run-mode` → `main` (R6 is optional/after).
