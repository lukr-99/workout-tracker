# Handoff Prompt — Phase 2: The logging loop + real screens (UI)

> Paste the block below as the opening prompt for the next agent. It is self-contained.

---

You are building **Phase 2 — the app's real UI and the core logging loop** — of the native-Android
**Workout Tracker** rewrite. Work on branch **`feature/app-rework`** in `F:\Code\workout-tracker`
(commit + push there). The frozen `.NET MAUI` app on `release/1.0` / tag `v1.0.0` is **read-only
reference**.

**What already exists on this branch (do not rebuild it):**
- **Phase 0** — Compose app shell that builds/installs on the phone (floating nav, dark theme in
  `ui/theme/`).
- **Phase 1** — the **data core**: Room + `WorkoutRepository` (the only IO boundary), pure
  `domain/` analytics, seeded catalog. See `docs/rework/phase-1-report.md`.
- **Phase 3 (merged)** — a full **data-services platform**: validated creation, composable
  query/stats, and import/export with an Android file UI. See `docs/rework/phase-3-report.md` and
  `docs/rework/07-data-services.md`. **You consume these; you do not reimplement them.**

## Read first (spec — follow it)

- `docs/rework/02-design-system.md` — **the decided shell + the component/motion/color spec.**
- `docs/rework/06-lyfta-study.md` + the screenshots in `docs/rework/reference/lyfta/` — the visual
  target for the Progress tab and component vocabulary (spline area chart, volume bars, row
  sparkline, stat card, muscle map).
- `docs/rework/04-feature-roadmap.md` — Phase 2 delivers **Milestone M1 (feature parity with the
  MAUI 1.0)** as native Compose; later milestones come after.
- `docs/rework/07-data-services.md` — the `AppContainer` service seams you wire into.
- `docs/rework/01-architecture.md` — conventions (one-file-per-screen, reusable visuals in
  `ui/components/`, repo is the only IO, `domain/` stays Android-free).
- Mirror the **ring-set** app's screen/component style: `lukr-99/ring-set`.

## The decided shell (implement this — replaces the Phase 0 flat 7-tab bar)

A **5-item bottom bar with a central ember Start action** (resolves phase-0-report §4.2):

```
[ Home ] [ History ] ( ＋ Start ) [ Progress ] [ Settings ]
```

- The center **＋ Start** is a raised ember button — **starts/resumes the live workout** (container-
  transform into the session), **not** a peer tab. Use `repository.createWorkoutSession(...)` (it
  returns the existing active session if one is live).
- **Templates + Catalog are NOT tabs** — they live in a **Library** surface reached from Home.
- **Progress** supersedes the old flat "Stats" tab. Update `ui/App.kt`'s `Screen` enum + `FloatingNav`
  accordingly (keep equal-width tabs; the center action is a distinct raised control).

## Scope — screens to build over the existing seams

Split `WorkoutViewModel` into per-area ViewModels (built via `AppContainer`); add the write
**actions** (the repo/services already support them — don't put data rules in the UI):

1. **Live Workout (the core loop)** — the highest-priority screen. Add exercise (snapshot via
   `repository.newEntryForExercise` **or** `workoutData.createSession`/entry drafts), log sets with
   `SetRow` + `NumberStepper` (reps × kg, RIR/RPE, set type, warmup, timed holds), reorder, rest
   timer (`RestTimerBar`, `exercises.defaultRestSeconds`), finish/discard with confirm. Persist via
   `repository.saveWorkoutSession`. Prefill "same as last time" from `getExerciseProgress`.
2. **Home (dashboard)** — active/resume card, quick-start, recent sessions, **Library** entry.
   Feed from `repository.getDashboardSnapshot()`.
3. **Library** (off Home) — **Templates** + `TemplateEditor`, **Catalog** + `ExerciseEditor`
   (search/filter via `observeExercises(filter)`, archive-not-delete, custom exercises).
4. **History** + **WorkoutDetail** — `observeHistory(search)`; full edit-after-the-fact via
   `saveWorkoutSession`.
5. **Progress** — build the Lyfta-grade analytics tab: overview stat cards + trend, a per-exercise
   list with inline sparklines (`Est 1RM` + delta), and per-exercise detail (spline `ProgressChart`
   + `VolumeBars`). **Drive it from `workoutData.calculateStats(StatsRequest)` / `querySessions` and
   `getExerciseProgress`** — do not compute stats in the UI.
6. **Settings** — theme/units/rest-timer via `settings/SettingsStore` (DataStore; still a stub —
   implement it), and a **"Data" row that routes to the provided `DataTransferScreen`** (import/
   export/backup). Create its VM with `DataTransferViewModel.factory(AppContainer.dataTransfer,
   AppContainer.documents)` — **do not build your own import/export UI.**

## Components to add (`ui/components/`, per design system)

`StatTile`, `SetRow`, `NumberStepper`, `ProgressChart` (smooth spline + area, scrub), `VolumeBars`
(dim current bar), `BodyHeatmap` (optional), `RestTimerBar`, `Chips`, `Dialogs`, `Toast`. Reuse the
existing `ScreenHeader`. Match the minimal-dark tokens; ember is the single accent.

## Explicit boundaries (Codex owns Phase 3 — don't collide)

- **Do not** parse CSV, build export rows, resolve import conflicts, or write query/stats engines in
  UI code. Route to `workoutData` / `dataTransfer` / `DataTransferScreen`.
- **Do not** edit files under `data/services/`, `data/transfer/`, `data/importer/`,
  `data/export/`, `domain/creation/`, `domain/query/`, `domain/stats/` except minimal, additive
  glue — flag anything you think needs changing in your report instead.
- `domain/` stays Android-free. The repository stays the only Room/file boundary.
- No Room schema change should be needed; if one is, add a `Migration` (baseline `1.json` is checked
  in) — never enable destructive fallback.

## Build / verify (environment notes that will save you time)

- Canonical SDK is `%LOCALAPPDATA%\Android\Sdk` (has build-tools **34/35**, platform android-35);
  the repo `local.properties` points there. `buildToolsVersion` is pinned to **35.0.0** — keep it.
- Gradle uses JDK 17 via `%USERPROFILE%\.gradle\gradle.properties`. If a build reports "SDK location
  not found / directory does not exist" despite a valid `local.properties`, it's a **stale daemon** —
  `.\gradlew.bat --stop` then rebuild (optionally `$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"`).
- Verify: `.\gradlew.bat testDebugUnitTest` + `.\gradlew.bat connectedDebugAndroidTest` (Galaxy A56),
  then `.\tools\build-and-install.ps1 -Launch`. Screenshot the live-logging loop with
  `.\tools\phone.ps1 screenshot`.

## Done when

The 5-item shell with the center Start action is live; a full **start → log sets → rest → finish**
loop persists real sessions through the repository; Home/Library/History/Progress/Settings read real
data; Progress renders the spline + volume charts from the stats service; Settings routes to
`DataTransferScreen`; unit + instrumented tests are green; it installs and runs on the A56. Then
write `docs/rework/phase-2-report.md` (same style as phase-1/3): what shipped, screenshots, any
seams you needed from Phase 3, and what a Phase 4 (polish/advanced features) should tackle.
