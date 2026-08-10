# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by Keep a Changelog, and this project currently uses simple semantic app versions for local releases.

## [2.2.0] - 2026-08-10

Run Mode reliability & map pass. First `2.x` entry recorded here; the `2.0`–`2.1` native-rewrite line
(Run Mode R0–R5) landed ahead of this changelog and is documented under `docs/run-mode/`.

### Fixed

- **Background & locked-screen tracking**: a partial wake lock now keeps the CPU awake for the duration of
  a run, so GPS fixes and the live clock keep flowing while the app is backgrounded or the phone is locked
  — instead of arriving in sparse bursts that showed up as skipped, straight-line paths.
- **Paused-and-walked segments no longer connect or count**: pausing, walking somewhere (traffic, a
  crossing), then resuming no longer joins the two ends on the map or adds the walked distance to your
  total. The trace breaks at each manual resume; distance, splits, and personal-record windows all skip
  the gap. Persisted via a new `run_points.segmentStart` flag (DB schema **v6**, additive migration).
- **Runs & routes are now in backups and exports**: the JSON export and the automatic backup only ever
  wrote strength data — runs and saved routes were silently omitted, so a restore couldn't bring them
  back. Both directions now round-trip Run Mode data (runs with their full traces, including pause
  breaks, plus saved routes). Existing backups made before this fix do **not** contain runs.

### Changed

- **Live map view**: the camera follows your heading (rotates so the road ahead is up) at a closer,
  street-level zoom, so the route you're on is legible mid-run. The recenter button re-arms both.
- **Live-run compass**: moved out from under the stats panel into the right-edge control group (next to
  the recenter and music buttons), and made a toggle — one tap locks the map to the phone's heading
  (road ahead up), the next returns it to north-up. The needle always points to true north; an ember
  tint means it's currently locked to your heading.

## [1.0.0] - 2026-07-25

### Milestone

- Marks the `.NET MAUI` implementation as the **frozen 1.0 proof-of-concept**. This version
  is preserved as a historical stepping stone on the `release/1.0` branch and the `v1.0.0` tag.
- Consolidates the 0.1–0.3 line: reliable offline logging, templates, live sessions, exercise
  catalog with `wger` sync, list-first browse screens with floating navigation, full editing of
  completed workouts, toast notifications, persisted theme selection, and JSON/CSV export.

### Notes

- Active development continues on `feature/app-rework`, a ground-up rewrite as a **native
  Android (Kotlin + Jetpack Compose)** app. See `docs/rework/` for the migration plan.
- The MAUI app remains buildable from this tag as a reference for behaviour and data shape.

## [0.1.0] - 2026-04-01

### Added

- Android-first `.NET MAUI` app scaffold with local Git and GitHub-backed repository setup
- Dark-theme shell navigation with pages for Home, Templates, Catalog, History, Workout Detail, Workout Editor, and Settings
- Core workout domain model for exercises, templates, sessions, entries, strength sets, cardio data, analytics snapshots, and exports
- Local SQLite persistence layer with seeded exercises and history-safe workout snapshots
- Custom exercise support plus manual sync/import support for public exercise metadata through `wger`
- JSON and CSV export services for future desktop-manager compatibility
- Basic analytics-ready queries for workout history, consistency, and exercise progression
- Repository tests covering initialization, template-to-session behavior, analytics, and export inclusion
- Project documentation covering architecture decisions and roadmap milestones

### Notes

- The current version is a foundation release focused on reliable offline logging and future compatibility.
- Progression graphs and richer statistics are planned on top of the existing history and analytics service layer in future milestones.
