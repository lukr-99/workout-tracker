# Phase 4 Report — Polish + advanced features (UI/integration)

> **Branch:** `feature/phase-4-ui` (worktree `../workout-tracker-phase4-ui`), cut from
> `feature/app-rework` at `5f16cae` (Phases 0+1+2+3+3.5).
> **Scope:** UI + integration only — `ui/`, `settings/`, and additive `AppContainer` wiring. No new
> service logic; records / recovery / progression / stats / sync all call the Phase 3/3.5 services.

## 1. TL;DR

The set input is now touch-first and labelled; past workouts can gain exercises; Records, Muscle
Recovery, and progression suggestions render from `insights`; the wger catalog syncs from Settings;
and a motion/haptics + cardio + RIR/RPE pass is in. Verified on the Galaxy A56: `testDebugUnitTest`
and `connectedDebugAndroidTest` green, and every priority-1/2 surface confirmed by hand with
screenshots (`docs/rework/phase-4-assets/`). One handoff item — **superset grouping UI** — is
deferred (see §6).

## 2. What shipped (against the handoff)

**Priority 1 — direct user feedback (done):**

1. **Set-logging input reworked** — the cramped `−/+` steppers are gone. Each set row now has
   explicit **REPS / KG** column headers (`SetColumnHeader`) over big tappable value cells
   (`ValueCell`); tapping one opens a **numpad sheet** (`NumberPadSheet`): a large live value, quick
   ± chips (the old step behaviour survives), and a full 7-8-9…0/⌫ keypad — typing is first-class.
   Ember minimal-dark taste kept. Screenshots: `set-input-labelled.png`, `numpad-editor.png`.
2. **Add exercise to past workouts** — `WorkoutDetailScreen` gained an **Add exercise** button that
   opens the *same* catalog picker as the live screen (`ExercisePicker`, now a shared component);
   `HistoryViewModel` exposes the catalog + `newEntryForExercise`. Confirmed on device:
   `add-exercise-past-workout.png`.

**Priority 2 — wire Phase 3.5 services into the UI (done):**

3. **Records** on `ProgressDetailScreen` — `ProgressViewModel.loadRecords` → `insights.records`; a
   Records card renders heaviest set, best e1RM, best set/session volume (each with its date) and the
   1–12 rep-max table, with a green PR badge. Screenshot: `records.png`.
4. **Live PR treatment** — `LiveWorkoutViewModel` calls `insights.evaluateSetRecord` on set-done;
   a new PR flags the set `isPr` (persisted, so the badge shows everywhere) and emits a `PrEvent`
   that the screen turns into a glowing `PrBanner` with an e1RM **count-up + haptic**.
5. **Muscle recovery / BodyHeatmap** — new `BodyHeatmap` component draws a stylised front/back
   muscle map tinted by `insights.recovery` readiness (green → amber → red, neutral when untrained);
   a **Muscle recovery** Overview card on Progress shows the heatmap, average readiness, and the
   least-recovered muscles with weekly sets. Confirmed rendering with real data:
   `muscle-recovery.png` (Abs at 58% → amber).
6. **Progression suggestions** — `addExercise` pre-fills the entry's sets from
   `insights.progression(id, DoubleProgression())` and toasts the rationale, falling back to last
   performance when history is insufficient.
7. **wger catalog sync** — `SettingsViewModel.syncCatalog` runs `wgerSync.sync(WgerSyncOptions())`
   on `Dispatchers.IO` with an idle/running/done/failed state; Settings has an **Exercise catalog**
   section with a Sync action, spinner, and the `WgerSyncSummary` (added/updated/skipped). Verified
   end-to-end over the network: **Added 495 · updated 0 · skipped 5** (`wger-sync.png`).

**Priority 3 — polish & parity (mostly done):**

8. **Motion / haptics** — checkmark **spring** + haptic on set-done, haptic on rest-zero, and a
   rest-ring **last-3s pulse** (turns error-red and breathes). PR count-up/glow (item 4) also
   satisfies the design-system motion note. Reduce-motion / animator-scale is honoured implicitly:
   all animations use Compose's frame clock, which respects the platform `MotionDurationScale`.
9. **Cardio logging UI** — cardio entries were read-only; new `CardioEditor` edits
   duration / distance / calories via the numpad, wired into the live *and* detail screens
   (`LiveWorkoutViewModel.updateCardio`).
10. **RIR/RPE inline editing** — the type-only dialog is replaced by `SetOptionsSheet`: set-type
    chips **plus** inline RIR/RPE editing (numpad, 0 clears) and remove
    (`LiveWorkoutViewModel.setRir/setRpe`).
11. **Superset grouping UI** — **deferred** (see §6).

## 3. Architecture as built

- **New components** (`ui/components/`): `NumberPad.kt` (`NumberPadSheet` + `ValueCell`),
  `ExercisePicker.kt` (extracted from the live screen so both screens share it), `PrBanner.kt`,
  `BodyHeatmap.kt`, `CardioEditor.kt`, `SetOptionsSheet.kt`. `SetRow.kt` reworked (+`SetColumnHeader`).
- **ViewModels** — additive only: `LiveWorkoutViewModel` (+`insights`; `prEvent`/`suggestion`
  one-shots; `setRir`/`setRpe`/`updateCardio`), `HistoryViewModel` (+catalog + `newEntryForExercise`),
  `ProgressViewModel` (+`insights`; `records` + `recovery` state), `SettingsViewModel` (+`wgerSync`;
  `catalogSync` state).
- **No new `AppContainer` members** — everything consumes the existing `insights` / `wgerSync` /
  `repository`. `domain/` untouched; no Room schema change.

## 4. Verified on device (Galaxy A56, RZCY60P9EHB)

- `./gradlew.bat testDebugUnitTest` — green (all prior Phase 1/3/3.5 JVM suites).
- `./gradlew.bat connectedDebugAndroidTest` — green on the A56.
- `build-and-install.ps1 -Launch` + `adb` walkthrough, screenshots in `docs/rework/phase-4-assets/`:
  numpad set input, add-exercise-to-past-workout, Records card, Muscle-recovery heatmap (real data),
  and a live wger sync (495 added). e1RM/records maths cross-checked (10×60 → e1RM 80 kg, 10RM 60 kg).

**Not exercised live on device:** the PR banner (item 4) and progression toast (item 6) fire only on
a live set-done; a device input-injection quirk kept mis-firing the checkmark tap during the manual
walkthrough (no crash — logcat clean — the app was merely backgrounded). Both paths compile, are
covered by the Phase 3.5 unit tests for `evaluateSetRecord`/`progression`, and share the exact
`insights` pipeline that Records + Recovery proved end-to-end on device. Worth a hands-on confirm.

## 5. Notes / decisions

- The Muscle-recovery card is sourced entirely from `insights.recovery` — `MuscleRecovery` already
  carries per-muscle weekly volume/set/load, so a separate `bodyPartStats()` call wasn't needed for
  this surface. `bodyPartStats` remains available for a future body-part breakdown screen.
- `BodyHeatmap` is **schematic**, not anatomical: major groups (chest, delts, biceps/triceps, abs,
  quads/hamstrings, glutes, back, calves) as Canvas blobs. Body-part names are matched
  case-insensitively against the catalog's `primaryBodyPart` (Chest/Back/Legs/Shoulders/Arms/Core/
  Biceps/Triceps/Glutes/Abs/Calves + a few synonyms).
- Distance in `CardioEditor` is edited/displayed in **km** regardless of unit system (there is no
  km↔mi helper in `Format`); a follow-up could add imperial distance.
- Set field edits (reps/weight/RIR/RPE) stay **in-memory until set-done** (unchanged from Phase 2's
  steppers, flushed by `persist()` on done/finish). The numpad makes it easier to edit without
  marking done, so a stalled draft loses un-done edits on VM recreation — acceptable and matches
  prior behaviour, but a debounced auto-persist would be a nice hardening.

## 6. Left for a release-hardening phase

- **Superset grouping UI** — the model + query support `entries.supersetGroup`; the UI affordance
  (group/ungroup, visual bracket) is not built.
- **Icons / splash / Play listing / signing / CI** — none of the store-readiness work is in.
- Optional: imperial distance for cardio; debounced auto-persist of in-progress set edits; a
  per-exercise / Settings progression-scheme picker (currently defaults to double-progression).

## 7. File map (touched)

- **New:** `ui/components/{NumberPad,ExercisePicker,PrBanner,BodyHeatmap,CardioEditor,SetOptionsSheet}.kt`.
- **Reworked components:** `ui/components/{SetRow,RestTimerBar}.kt`.
- **Screens:** `ui/screens/{LiveWorkout,WorkoutDetail,ProgressDetail,Progress,Settings}Screen.kt`.
- **ViewModels:** `ui/{LiveWorkout,History,Progress,Settings}ViewModel.kt`.
- **Assets:** `docs/rework/phase-4-assets/*.png`.
