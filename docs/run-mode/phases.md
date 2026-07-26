# Run Mode — Phased build plan

Each phase is a shippable vertical slice on `feature/run-mode`, verified on the Galaxy A56. Same
conventions as the strength rework (repo = only IO boundary, `domain/` Android-free, additive
non-destructive migrations, on-device verification + a phase report). Phases are handoff-sized for an
agent; R0–R2 are the critical path to "record a real run."

## R0 — Foundation
**Goal:** the plumbing + an empty Run screen with a live dark map centered on you.
- Shell integration per README decision (recommended: center `＋ Start` → Lift/Run chooser; `Runs`
  tab replaces `History`).
- Location permission flow (fine location + notifications), just-in-time with rationale.
- `data/map` MapView component (chosen provider) rendering a dark basemap; follow current location.
- Run data model + **Room migration v5** (`runs`, `run_points`, `routes`, `route_points`; `5.json`
  checked in; migration test `…→5`). `RunRepository` skeleton + polyline codec (unit-tested).
- `domain/run/RunModels` + `Pace` math (unit-tested).
**Exit:** open Runs → Run screen shows the map tracking your location; DB migrates cleanly; tests green.

## R1 — Live run core (critical)
**Goal:** record, pause, finish, and save a real run.
- `LocationService` foreground service (`location` type) + ongoing notification with pause/stop.
- `RunTracker`: distance, elapsed vs moving time, current/avg pace, **splits** (km/mi), GPS
  jitter/accuracy filtering, **auto-pause**.
- `LiveRunScreen`: live map with the growing **ember polyline**, big metrics, start/pause/resume/
  finish, countdown, survives backgrounding/process-death (service owns + persists the trace).
- Save run (trace + encoded polyline) via `RunRepository`.
**Exit:** run outside, screen off, come back — the run is tracked, paused correctly, saved with an
accurate distance/trace. Crash buffer clean.

## R2 — Run detail, history & stats
**Goal:** runs are browsable and feed statistics.
- `RunsScreen` hub (recent runs w/ map thumbnails + key stats) and `RunDetailScreen` (full map,
  **split table**, **pace + elevation charts**, edit/delete).
- `domain/run/RunStats` (pure): weekly/monthly distance, avg-pace trend, **PRs** (fastest 1k/5k/10k/
  half, longest, most elevation), consistency.
- **Progress** gains a Running section (reuse `ProgressChart`/`VolumeBars`/`StatTile`).
- **Health Connect** write: `ExerciseSessionRecord` (Running) + `ExerciseRoute`, idempotent by
  fingerprint (extend `data/health`). Runs optionally linked to a Cardio `WorkoutSession` for unified
  history.
**Exit:** past runs render with maps + splits; Progress shows running distance/pace/PRs; a run appears
in Health Connect.

## R3 — Route planning & saving
**Goal:** plan, save, and follow routes.
- `RoutePlannerScreen`: tap waypoints → **snap to roads/paths** via the routing client → live
  distance + **elevation profile**; drag/undo; **save** as a named route.
- Start a run **from a saved route** (show the planned line under the live trace) with **off-route**
  awareness (distance-from-route indicator; optional cue).
- Saved routes list + reuse; offline tile caching for a route's region.
**Exit:** plan a route on-device, save it, start a run that follows it and flags going off-route.

## R4 — Spotify
**Goal:** control music without leaving the run.
- `SpotifyController` (App Remote): connect, current track + art, **play/pause/skip** from a
  `RunMiniPlayer` on `LiveRunScreen`; one-time auth; client id in a git-ignored `spotify.properties`.
- Fallback path: a deep-link that just opens Spotify (if App Remote is deferred).
**Exit:** during a live run, see the current track and skip/pause it in-app (or one-tap open Spotify).

## R5 — Polish
- Audio/haptic **split cues** + finish summary voice/haptic; auto-pause tuning; start **countdown**.
- **GPX import/export**; extend `ExportBundle` to **1.5** (runs + routes).
- Offline tile caching UX; **share-run card** (map + stats image); **home-screen widget** / quick-start
  tile; reduce-motion/units respect.
**Exit:** cues fire, GPX round-trips, a run can be shared as an image.

## R6 — Optional / advanced
- Heart rate via **BLE** or Health Connect live; cadence via `ACTIVITY_RECOGNITION`/step sensor.
- **Segments** (define + auto-detect + leaderboard-vs-self); **ghost/route racing** (pace vs a past
  run of the same route); interval/workout runs.

## Cross-cutting / done-criteria
- Every phase: on-device verification (`tools/build-and-install.ps1 -Launch`, `tools/phone.ps1`),
  green `testDebugUnitTest` + `connectedDebugAndroidTest`, and a `docs/run-mode/phase-R#-report.md`.
- Ship the whole thing as **v2.1.0**; merge `feature/run-mode` → `main` + tag when R0–R5 land (R6
  optional/after).
- **Build note:** the LOCALAPPDATA SDK is currently partly on a detached `E:` drive — build against
  `C:\Program Files (x86)\Android\android-sdk` (platform 35 + build-tools 36; local `buildToolsVersion`
  override) or reconnect `E:` / reinstall `build-tools;35.0.0` + `platforms;android-35`. See the
  build-toolchain memory.
