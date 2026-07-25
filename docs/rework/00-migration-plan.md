# Migration Plan

From the `v1.0.0` MAUI proof-of-concept to a shipping native Android app, in reviewable phases.
Each phase ends in something buildable and demonstrable on the phone.

## Guiding rules

- **The MAUI app is frozen, not deleted.** It stays on `release/1.0` + `v1.0.0` as the reference
  for behaviour and data shape. We port *from* it; we don't edit it.
- **Rewrite, don't transpile.** We reimplement the domain in idiomatic Kotlin; we do not try to
  mechanically convert C#. The MAUI `Domain/Models.cs` and `Data/` are the spec.
- **Vertical slices.** Each phase delivers a working end-to-end path, not a horizontal layer.
- **Data compatibility is a feature.** The new app must be able to import a `v1.0.0` JSON export
  and a Lyfta CSV before we call the migration "done".

## Phase 0 — Scaffold (foundation)

Goal: an empty but well-structured app that builds and installs.

- Gradle (Kotlin DSL) project, version catalog, Compose + Material 3, Room, DataStore,
  kotlinx.serialization.
- `MainActivity` hosting an `App()` scaffold with the floating-nav `Screen` enum and empty
  screens.
- Theme wired up from [02-design-system.md](02-design-system.md) (dark default).
- `tools/build-and-install.ps1` builds and installs a debug APK on the phone.
- **Exit check:** app installs, launches, shows the nav and an empty themed Home.

## Phase 1 — Data core

Goal: the domain and storage layer, headless but tested.

- Port `Domain/Models.cs` → Room `Entities.kt` (see [03-data-model.md](03-data-model.md)).
- `WorkoutDao`, `WorkoutDb` (+ type converters), `WorkoutRepository`.
- Port `SeedExercises.cs` → `Seed.kt`; seed on first run.
- `domain/` analytics + estimates + units, ported from the MAUI service queries, as pure Kotlin.
- Unit tests for domain; instrumented tests for DAOs.
- **Exit check:** tests green; a debug screen can insert a session and read it back.

## Phase 2 — Core logging loop (the reason the app exists)

Goal: create → log → finish → review a workout.

- **Catalog:** browse/search/filter exercises; add/edit/archive custom exercises.
- **Templates:** create/edit a template; reorder exercises.
- **Live workout:** start from scratch or template; add exercises; inline strength set editing
  (reps × kg, RIR/RPE) with steppers; cardio duration/distance/calories; running duration;
  finish / discard with confirmation.
- **History + detail:** list completed sessions; open a session; edit after the fact.
- **Exit check:** a full workout can be logged and reviewed entirely on-device, dark-themed.

## Phase 3 — Import / export (data compatibility gate)

Goal: get existing data in, and never trap it.

- `ExportBundle` JSON export + import (round-trip / restore).
- CSV export for spreadsheets.
- **Lyfta CSV importer** with field mapping + dedupe (see [05-lyfta-import.md](05-lyfta-import.md)).
- MAUI `v1.0.0` JSON export importer (so nothing from the POC is lost).
- Share-sheet + Storage Access Framework (open/save document) integration.
- **Exit check:** import a real Lyfta export and a real `v1.0.0` export; history looks right.

## Phase 4 — Visual progression (the Lyfta-grade payoff)

Goal: the app *feels* like Lyfta, with real analytics.

- Home dashboard: active workout, streaks, recent, weekly volume.
- Exercise progression charts (best set / e1RM / volume over time) with scrub.
- Consistency views (frequency, streaks, weekly/monthly).
- Body-part distribution; PRs and milestone surfaces.
- Motion pass: transitions, the rest-timer surface, toast feedback.
- **Exit check:** side-by-side with Lyfta it reads as a peer, in our own dark style.

## Phase 5 — Polish & release prep

- Rest timers, supersets, per-block notes, unit system (kg/lb).
- Accessibility pass (touch targets, content descriptions, contrast).
- App icon, adaptive icon, Play-style metadata.
- Release signing config; `tools/` release build path.
- Decide final `applicationId` and whether to publish.
- **Exit check:** a signed release build a daily-driver would actually use.

## Cutover

- The new app is the daily driver once Phase 3 lands (data safely imported).
- The MAUI app is retired to reference-only; `main` moves to the Kotlin app when
  `feature/app-rework` merges.
- Post-merge, `docs/decisions.md` and `docs/roadmap.md` are updated to reflect the native app;
  the MAUI-specific docs are archived under `docs/legacy-maui/`.

## Sequencing note

Phases 0–1 can proceed immediately and in parallel with the **Lyfta extraction** (which only
needs the phone + an in-app export, not our app). Getting the real Lyfta CSV early de-risks the
Phase 3 mapping.
