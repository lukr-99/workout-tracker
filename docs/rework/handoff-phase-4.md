# Handoff Prompt — Phase 4: Polish + advanced features (UI/integration)

> **Status: ✅ COMPLETED & MERGED** into `feature/app-rework` — see [phase-4-report.md](phase-4-report.md).
> Kept for history. Remaining items (superset UI, Health Connect grant flow, store-readiness) moved
> to the release-hardening backlog in the README.

> Paste the block below as the opening prompt for the next agent. It is self-contained.

---

You are building **Phase 4 — polish and advanced features** of the native-Android **Workout Tracker**
rewrite. The app is already a working tracker: `feature/app-rework` carries **Phases 0+1+2+3+3.5** —
a 5-item shell with a central Start action, a full live-logging loop persisting through Room, and
Home / Library / History / Progress / Settings over the data + services layer. Work on branch
**`feature/app-rework`** in `F:\Code\workout-tracker` (commit + push there). The frozen `.NET MAUI`
app on `release/1.0` / tag `v1.0.0` is read-only reference.

## Read first

- `docs/rework/phase-2-report.md` — **§6 is your backlog** (this prompt expands it) + the file map of
  every screen/component/VM you'll edit.
- `docs/rework/phase-3.5-report.md` — **the exact `AppContainer.insights` / `AppContainer.wgerSync`
  APIs (signatures + examples)** you wire into the UI. Do not reimplement any of that logic.
- `docs/rework/02-design-system.md` — the design system (esp. **§Motion** and the component specs)
  and the decided shell.
- `docs/rework/06-lyfta-study.md` + `docs/rework/reference/lyfta/*.png` — the visual targets
  (Muscle Recovery card, per-exercise Records/Progress, the set-logging UX to beat).
- Mirror **ring-set** conventions.

## Priority 1 — direct user feedback from the Phase 2 hands-on (do these first)

1. **Rework the set-logging input to be mobile-intuitive** (`ui/components/SetRow.kt`,
   `NumberStepper.kt`). The `−/+` steppers are poor touch input and the two columns aren't labelled.
   Add explicit **REPS / KG** column headers, make the value tappable to open a **big numpad-style
   inline editor** (keep quick ±, but typing must be first-class), and keep the ember minimal-dark
   taste. This is the single most-important Phase 4 item.
2. **Add "add exercise" to past-workout editing** (`ui/screens/WorkoutDetailScreen.kt`,
   `ui/HistoryViewModel.kt`). `WorkoutDetail` can edit/remove existing entries/sets but can't add a
   new exercise to a finished session — wire the **same catalog picker used in the live screen**
   (the `HistoryViewModel` needs the catalog + `repository.newEntryForExercise`).

## Priority 2 — wire the Phase 3.5 services into the UI (they exist, nothing renders them yet)

Consume `AppContainer.insights` and `AppContainer.wgerSync` (see phase-3.5-report for signatures):

3. **Records** on the per-exercise Progress detail (`ui/screens/ProgressDetailScreen.kt`): render
   `insights.records(exerciseId)` — heaviest set, best e1RM, best set/session volume, the 1–12
   **rep-max table**, each with its source date. Add a "Records" section/tab like Lyfta's.
4. **Live PR treatment** in the logging loop (`LiveWorkoutViewModel` + `SetRow`): on set-done call
   `insights.evaluateSetRecord(exerciseId, set)` and, when a new PR, fire the **PR count-up +
   `positive` glow + haptic** (also satisfies the motion item below).
5. **Muscle-recovery / body-part surface** — build the deferred **`BodyHeatmap`** component
   (front/back muscle map) and a Home/Progress **"Muscle Recovery" card** from
   `insights.recovery(...)` (per-muscle 0–100 readiness) and `insights.bodyPartStats()` (weekly
   volume/sets per body part). This is Lyfta's standout Overview card.
6. **Progression suggestions** in the live screen: when adding an exercise, offer
   `insights.progression(exerciseId, scheme, deload)` as the **suggested next sets** (prefill +
   rationale). Let the user pick a scheme in Settings or per-exercise; default to double-progression.
7. **wger catalog sync** in Settings/Library: a "Sync exercise catalog" action calling
   `wgerSync.sync(WgerSyncOptions(...))` with progress + the `WgerSyncSummary` result
   (added/updated/skipped) in a toast/sheet. It needs network (INTERNET perm already added).

## Priority 3 — polish & remaining parity

8. **Motion pass** (`02-design-system.md §Motion`): checkmark spring on set-done, **container-
   transform** for the FAB → live-session expand (currently a plain overlay push), smooth screen
   cross-fades, rest-timer ring drain + last-3s pulse. Respect **reduce-motion** and the system
   animator-scale; add **haptics** on set-done and rest-zero.
9. **Cardio logging UI** — cardio entries currently render read-only; build a duration/distance/
   calories editor on the live + detail screens (`CardioEntryData` exists in the model).
10. **RIR/RPE inline editing** on `SetRow` (today only set **type** is editable via the badge dialog).
11. **Superset grouping UI** — the data model + query already support `entries.supersetGroup`; add
    the grouping affordance in the live screen.

## Constraints

- Stay in `ui/`, `settings/`, and additive `AppContainer` wiring. **Do not** reimplement records,
  recovery, progression, stats, import/export, or sync in the UI — call `insights` / `wgerSync` /
  `workoutData` / `dataTransfer`. Flag any service gap in your report instead of working around it.
- `domain/` stays Android-free; the repository stays the only Room/file boundary.
- No Room schema change expected. If one is truly needed, add a `Migration` (baseline `1.json` is
  checked in; there's a `WorkoutMigrationTest` scaffold) — never enable destructive fallback.

## Build / verify (environment notes)

Canonical SDK `%LOCALAPPDATA%\Android\Sdk` (build-tools **35.0.0**, kept pinned); JDK 17 via
`~/.gradle/gradle.properties`. If Gradle says "SDK location not found" despite a valid
`local.properties`, it's a **stale daemon** — `.\gradlew.bat --stop` then rebuild (optionally
`$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"`). Verify with `.\gradlew.bat testDebugUnitTest`
+ `connectedDebugAndroidTest` (Galaxy A56), then `.\tools\build-and-install.ps1 -Launch` and
`.\tools\phone.ps1 screenshot`.

## Done when

The set input is touch-friendly and labelled; past workouts can gain exercises; Records, Muscle
Recovery, and progression suggestions render from `insights`; catalog sync works from the UI; the
motion/haptics pass is in; cardio + RIR/RPE + superset UI exist. Tests green, installs + runs on the
A56 with screenshots. Then write `docs/rework/phase-4-report.md` (same style) and note anything left
for a release-hardening phase (icons/splash, Play listing, signing, CI).
