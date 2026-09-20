# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by Keep a Changelog, and this project currently uses simple semantic app versions for local releases.

## [2.5.2] - 2026-09-20

### Fixed

- **In-app update failed to install**: the updater never checked that a download actually finished.
  A dropped connection simply ends the response stream without raising anything, so a partial APK
  was handed to Android's package installer, which rejected it as corrupt — looking like an install
  problem rather than the half-finished download it was. The download now verifies the transferred
  byte count against the size GitHub reports, checks the HTTP status, streams to a `.part` file that
  is only put in place once complete, and deletes it otherwise.
- **Silent updater failures**: every error was discarded by `runCatching { }.getOrNull()`, leaving
  "Download failed — try again later." and nothing in the logs. Failures now show the actual reason
  and are logged.

### Changed

- **Update download is ~3x smaller**: release builds ship only `arm64-v8a`. The universal APK
  carried native libraries for four architectures — 49 MB of its 52 MB — so roughly 39 MB of every
  update was code no phone could run. Debug builds keep every ABI so emulator tests still run.
- The Updates section now reports real download progress instead of an indeterminate spinner.

## [2.5.1] - 2026-09-20

### Fixed

- **Live logging crash**: marking sets done could kill the app outright. When a set earned a
  personal record, its "PR" chip appeared in the exercise card, and the card measured its height
  with intrinsics to size the superset rail — which a lazily-laid-out chip strip cannot answer, so
  Compose threw `IllegalStateException`. Because the PR flag is persisted into the draft, reopening
  the app crashed it again within seconds, leaving a force-stop as the only way out. The chip strip
  no longer uses a lazy list, and the superset rail is painted rather than measured, so neither
  half of the fault can recur. The same crash could be triggered by any tagged set, not just a PR.

## [2.5.0] - 2026-08-30

### Added

- **Per-exercise KG/LB input**: tap the unit chip on any live exercise to enter the machine's shown
  weight directly; storage remains metric and other exercises keep their own unit choice.
- **Combinable set tags**: warm-up, drop, to failure, failed early, negative, and back-off can now be
  selected together. Legacy single set types remain compatible with older history and exports.
- **Exercise lifecycle**: exercises can be started, timed, finished, collapsed into a compact stats
  summary, expanded again, or reopened. Logging the first set starts its timer automatically.
- **Superset editor**: clearer position/count visuals plus controls to add adjacent exercises,
  remove individual members, or ungroup the block.
- **Exact share preview**: workout detail now shows the rendered postcard before opening Android's
  share sheet, and exercise cards expose sets, reps, volume, best set, and elapsed time.
- **Guided Windows installer**: a release-packaging command creates a double-click installer for a
  USB-connected Android phone, including first-time USB-debugging guidance.

### Data

- Room schema **v7** adds non-destructive exercise timing/unit fields and set-tag JSON. Portable
  exports are now **v1.6** and still accept every published format from v1.0 onward.

## [2.4.0] - 2026-08-24

### Fixed

- **Live logging**: a completed ("done") set now persists via the set's `performedAtUtc`, so its
  checkmark — and any reps/weight typed just before — survive leaving the screen or the OS reclaiming
  the process. The draft is also flushed when the app is backgrounded.
- **Exercise picker**: the catalog list now owns its scroll gesture, so a fling no longer drags the
  bottom sheet closed mid-scroll without a selection.
- **Run + lift**: when a run and a lift are both live, the centre action asks which to resume instead
  of always reopening the run (the lift was previously unreachable).
- **PR celebration**: the personal-record banner is now opaque, elevated, sits clear of the top bar,
  and stays visible longer.
- **Exercise art / sync**: broader image matching for imported/custom names (singularised-token
  fallback), and wger sync now resolves site-relative image URLs to absolute.

### Added

- **Shareable workout postcard**: a past workout can be shared as a branded image summarising volume,
  sets, exercises, time and any personal records (Workout detail → Share).

## [2.3.0] - 2026-08-22

### Added

- **In-app updater**: Settings → *Updates* now checks GitHub Releases for a newer build and, when one
  is available, downloads its APK and hands it to the system installer. No third-party dependencies
  (`HttpURLConnection` + `org.json`). In-place installs require the release APK to be signed with the
  same key as the installed build, so the first release APK is installed manually; thereafter it
  upgrades in place. Adds the `REQUEST_INSTALL_PACKAGES` permission and a FileProvider `updates/` path.

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
