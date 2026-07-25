# Phase 2 Report — The logging loop + real screens (UI)

> **For:** the Phase 4 agent (polish + advanced features), and anyone reviewing the UI.
> **From:** the Phase 2 UI agent.
> **Branch:** `feature/app-rework` · **Date:** 2026-07-26
> **Spec followed:** `docs/rework/02-design-system.md`, `04-feature-roadmap.md` (Milestone M1),
> `07-data-services.md`, `01-architecture.md`; visual target `06-lyfta-study.md`; mirrored
> `lukr-99/ring-set`.

---

## 1. TL;DR

Phase 2 is **complete and verified on real hardware (Galaxy A56)**. The app is now a real workout
tracker: the flat 7-tab Phase-0 shell is replaced by the **decided 5-item bar with a central ember
Start action**, and a full **start → add exercise → log sets → rest → finish** loop **persists real
sessions through the repository**. Home, Library, History (+ detail), Progress (+ per-exercise
detail), and Settings all read and write live data over the Phase 1/3 seams. No data rules were
reimplemented in the UI.

- **`.\gradlew.bat testDebugUnitTest`** → green (Phase 1/3 domain + transfer suites, unchanged).
- **`.\gradlew.bat connectedDebugAndroidTest`** → **11/11 green** on the A56.
- **`.\gradlew.bat assembleDebug`** + `tools\build-and-install.ps1 -Launch` → installs, launches.
- **On-device loop verified**: created a Quick Workout, added an exercise from the seeded catalog,
  logged sets (steppers + "Set logged" toast + live volume), finished, and confirmed it landed in
  **History** and drove the **Progress** KPIs (`1 workout`, `streak 1`). Screenshots in
  `docs/rework/phase-2-assets/`.

The only Phase 3 file touched was `data/AppContainer.kt` (additive: expose `SettingsStore`). Nothing
under `data/services|transfer|importer|export`, `domain/creation|query|stats` was modified.

---

## 2. What shipped (against the handoff's scope)

| # | Handoff scope | Status | Where |
|---|---------------|--------|-------|
| — | 5-item shell + central ember Start action (resolves phase-0 §4.2) | ✅ | `ui/App.kt`, `ui/Navigation.kt` |
| 1 | **Live Workout** — add exercise, log sets, reorder, rest timer, finish/discard, prefill | ✅ | `ui/screens/LiveWorkoutScreen.kt`, `ui/LiveWorkoutViewModel.kt` |
| 2 | **Home** dashboard — resume/quick-start/recent/Library entry | ✅ | `ui/screens/HomeScreen.kt`, `ui/HomeViewModel.kt` |
| 3 | **Library** — Templates + `TemplateEditor`, Catalog + `ExerciseEditor` | ✅ | `ui/screens/LibraryScreen.kt`, `TemplateEditorScreen.kt`, `ExerciseEditorScreen.kt`, `ui/LibraryViewModel.kt` |
| 4 | **History** + **WorkoutDetail** (edit-after-the-fact) | ✅ | `ui/screens/HistoryScreen.kt`, `WorkoutDetailScreen.kt`, `ui/HistoryViewModel.kt` |
| 5 | **Progress** — KPIs + weekly trend + per-exercise list/detail | ✅ | `ui/screens/ProgressScreen.kt`, `ProgressDetailScreen.kt`, `ui/ProgressViewModel.kt` |
| 6 | **Settings** (real `SettingsStore`) + **Data** route to `DataTransferScreen` | ✅ | `ui/screens/SettingsScreen.kt`, `ui/SettingsViewModel.kt`, `settings/SettingsStore.kt` |
| — | Components: `StatTile`, `SetRow`, `NumberStepper`, `ProgressChart`, `VolumeBars`, `RestTimerBar`, `Chips`, `Dialogs`, `Toast` | ✅ | `ui/components/` |

`BodyHeatmap` was left out (marked optional in the handoff) — noted for Phase 4.

---

## 3. Architecture as built

### The shell (`ui/App.kt` + `ui/Navigation.kt`)

`Screen`'s flat enum is gone. The shell is now:

```
[ Home ] [ History ] ( ＋ / ▶ Start ) [ Progress ] [ Settings ]
```

- Four peer **`Tab`s** cross-fade inside one always-mounted layer (tab state persists).
- The center is a **raised ember circle**, not a tab. It calls `repository.createWorkoutSession(...)`
  (which returns any existing active session) and pushes the live screen. When a session is live it
  flips to a **▶ Resume** glyph, driven by `observeActiveSession()`.
- Full-screen flows (LiveWorkout, Library, both editors, WorkoutDetail, ProgressDetail,
  DataTransfer) layer over the tabs through a **manual back-stack `Navigator`** (ring-set style, no
  Navigation-Compose graph) with `BackHandler` wired to `pop()`.
- `LocalToast` + `ToastHost` provide the app-wide confirmation toast; theme (system/dark/light
  override) is resolved in `MainActivity` from `SettingsStore` and passed to `WorkoutTheme`.

### ViewModels (per-area, built via `AppContainer`)

`WorkoutViewModel` (the Phase-1 placeholder) was split into `Home`, `LiveWorkout`, `Library`,
`History`, `Progress`, and `Settings` ViewModels, each with a `factory(container)` and constructed
with `viewModel(factory = …)` in `App`. They consume the Phase 1/3 seams directly:

- **creation/validation** → `workoutData.createExercise/createTemplate` (`CreationResult` surfaces
  issues as toasts);
- **stats** → `workoutData.calculateStats(StatsRequest)` for Progress KPIs and the weekly-volume
  trend (`DimensionKeys.Week` × `MetricKeys.VolumeKg`), and `querySessions` for the per-exercise
  series (e1RM/volume via `domain.Estimates`);
- **persistence** → always `repository.*`; the UI never touches Room/files.

### The live-logging model (the core loop)

`LiveWorkoutViewModel` keeps an **in-memory working draft** of the active session so steppers stay
responsive, and **flushes to the repository** on structural changes and on marking a set done (so a
live session survives app restart — verified: backgrounding mid-set and re-opening resumed it).
Field edits (reps/weight) are in-memory until the next flush; `persist()` is deliberately
fire-and-forget (ids are assigned client-side and the repo preserves them, so reading the save back
would clobber concurrently-typed values). "Same as last time" prefills a new exercise's sets from
the most recent completed session containing it. The rest timer ticks in `viewModelScope`; its
default is `exercise.defaultRestSeconds ?? settings.defaultRestSeconds`.

---

## 4. Verified on device

| Step | Result |
|---|---|
| 5-item shell + center Start | renders; `home.png` |
| Start → active session created, container into live screen | ✅ |
| Add exercise from seeded catalog (bottom-sheet picker, searchable) | ✅ `exercise-picker.png` |
| Log set → stepper edits, "Set logged" toast, live volume, done-state | ✅ `live-logging.png` |
| Rest timer auto-starts on set done | ✅ |
| Background mid-session → Home shows **IN PROGRESS** resume card + ▶ Resume | ✅ |
| Finish (confirm dialog) → persists, drops empty entries | ✅ |
| Session appears in **History** with rollup | ✅ `history.png` |
| **Progress** KPIs + weekly bars + per-exercise list from the stats service | ✅ `progress.png` |
| Settings theme/units/rest write to DataStore; **Data** routes to `DataTransferScreen` | ✅ |

Tests: `connectedDebugAndroidTest` → **11/11** on `SM-A566B`; `testDebugUnitTest` green.

---

## 5. Seams consumed from Phase 3 (no new ones needed)

Everything wired cleanly through the existing `AppContainer` entry points. **One additive change:**
`AppContainer` now exposes `settings: SettingsStore` (the Phase-0 stub became a real DataStore in
`settings/SettingsStore.kt`). No Phase 3 service signature needed changing, and no Room schema
change was required (baseline `1.json` untouched, no migration added).

---

## 6. What Phase 4 (polish / advanced) should tackle

**Direct user feedback from the Phase 2 hands-on review (highest priority):**

1. **Rework the set-logging input to be more mobile-intuitive.** The reps × weight `−/+`
   `NumberStepper` is not the best touch input, and the two columns aren't clearly labelled
   ("don't know which is which"). Add explicit **REPS / KG** column headers and/or a bigger
   numpad-style inline editor; keep the ember minimal-dark taste. (`ui/components/SetRow.kt`,
   `NumberStepper.kt`.)
2. **Add an "add exercise" action to past-workout editing.** `WorkoutDetailScreen` can edit/remove
   existing entries and sets but can't add a new exercise to a finished session — wire the same
   picker used in the live screen (needs the catalog on `HistoryViewModel`).

**Other follow-ups:**

3. `BodyHeatmap` (front/back muscle map) — deferred optional component.
4. Motion polish per 02-design-system §Motion: PR count-up + `positive` glow, checkmark spring,
   container-transform for the FAB→live-session expand (currently a plain overlay push).
5. Cardio logging UI — cardio entries render read-only; the live screen shows "Cardio entry" without
   a duration/distance editor yet.
6. Reduce-motion / animator-scale respect; haptics on set-done and rest-zero.
7. RIR/RPE inline editing (today only set **type** is editable via the badge dialog).
8. Superset grouping UI (the data model + query support it; no UI yet).

---

## 7. File map

- **Shell/nav:** `ui/App.kt`, `ui/Navigation.kt`, `MainActivity.kt`.
- **ViewModels:** `ui/{Home,LiveWorkout,Library,History,Progress,Settings}ViewModel.kt`
  (+ existing `DataTransferViewModel.kt`).
- **Screens:** `ui/screens/{LiveWorkout,Home,Library,TemplateEditor,ExerciseEditor,History,
  WorkoutDetail,Progress,ProgressDetail,Settings}Screen.kt` (+ provided `DataTransferScreen.kt`).
- **Components:** `ui/components/{Format,StatTile,SectionCard,Chips,Dialogs,Toast,NumberStepper,
  SetRow,RestTimerBar,Charts}.kt` (+ existing `ScreenHeader.kt`).
- **Settings:** `settings/SettingsStore.kt`; **DI:** `data/AppContainer.kt` (+`settings`).
- Removed: the placeholder `ui/WorkoutViewModel.kt` and the Phase-0 stub screens.
