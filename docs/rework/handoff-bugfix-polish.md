# Handoff Prompt — Sub-phase: Bug-fix & UX polish (crash + navbar + exercise search/images)

> **Status: ✅ COMPLETED & MERGED** into `feature/app-rework` — see
> [bugfix-polish-report.md](bugfix-polish-report.md). Kept for history.


> Paste the block below as the opening prompt for the agent (Codex — Claude usage is tight). It is
> self-contained. This sub-phase touches **both `data/` and `ui/`**.
>
> **Do item 1 (the crash) FIRST and ship it** — the app currently crashes on every set-done, so it's
> unusable until this lands. It must be fixed before Phase 5 release hardening
> ([handoff-codex-phase-5-release.md](handoff-codex-phase-5-release.md)).

---

You are fixing bugs and small UX gaps found while using the current **Workout Tracker** build
(Phases 0–4 merged on `feature/app-rework`, `F:\Code\workout-tracker`). Work on `feature/app-rework`
directly (small, targeted changes) or a short-lived `fix/bugfix-polish` branch to merge back. The
frozen MAUI app on `release/1.0` is read-only reference. Match the existing design system
(`docs/rework/02-design-system.md`) and reuse `ui/components/*`; consume `repository`/`insights` — do
not reimplement data rules in the UI.

## 1. P0 CRASH — finishing a set crashes the app (FK constraint failure)

**Repro:** Start a workout → add an exercise → enter numbers on two sets → tap the checkmark (done)
on the first set → **app crashes**.

**Captured stack trace (from the device `crash` buffer):**
```
FATAL EXCEPTION: main   Process: com.lukr99.workout
android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY[787])
  at androidx.room.EntityInsertionAdapter.insert(...)
  at androidx.room.EntityUpsertionAdapter.upsert(...)
  at com.lukr99.workout.data.WorkoutDao_Impl$31.call(WorkoutDao_Impl.java:806)   // a child upsert
  … via CoroutinesRoom … TransactionExecutor
```

**Root cause (diagnosed):** `WorkoutRepository.saveWorkoutSession` (`data/WorkoutRepository.kt:252`)
→ `replaceSessionChildren` (`:274`) is **not atomic**. It does, as *separate* Room transactions:
`dao.upsertSession` → `dao.deleteEntriesForSession` (cascades sets/cardio) → `dao.upsertEntries` →
`dao.upsertStrengthSets` (`:299–301`). Meanwhile the live screen's `persist()` in
`ui/LiveWorkoutViewModel.kt` is **fire-and-forget** and is called on *every* structural change **and**
on set-done. Entering a 2nd set and then marking a set done fires **two overlapping `saveWorkoutSession`
coroutines**: one call's `deleteEntriesForSession` removes the entries while the other is between
`upsertEntries` and `upsertStrengthSets`, so a `strength_sets` row is inserted whose parent `entries`
row no longer exists → **FK 787**.

**Fix (both, please):**
1. **Make the save atomic.** Wrap the whole `saveWorkoutSession` body (session upsert +
   `replaceSessionChildren`) in the existing `inTransaction { }` helper (Codex added
   `TransactionRunner`/`inTransaction` in Phase 3). A single Room transaction serializes on the writer
   connection, so delete→re-insert is atomic and a second save runs fully after the first.
2. **Serialize `persist()` in the ViewModel** so saves can't overlap: guard with a `Mutex` (or a
   conflated single-flight `Channel`/`actor`) and cancel/supersede an in-flight persist. This also
   avoids lost writes and reduces churn.

**Verify:** reproduce the exact flow on the A56 (two sets, mark first done) — no crash; the set
persists and shows done. Add a regression test: a `WorkoutRepositoryTest` (instrumented) that saves a
session, then concurrently calls `saveWorkoutSession` twice with overlapping children and asserts no
exception + consistent rows. Confirm the `crash` buffer is clean: `adb logcat -b crash -c` then retest.

## 2. Navbar — "Start" label under the center ＋ is not centered

In `ui/App.kt` (the `FloatingNav` / center Start control), the **Start** text under the raised ember
＋ button is horizontally misaligned vs. the four tab labels. Align it: center the label under the FAB
(account for the FAB's raised offset / different container), matching the icon+label rhythm of the
peer tabs. Check on-device with `.\tools\phone.ps1 screenshot`.

## 3. Exercise search + filtering

The catalog picker and Library catalog should be searchable/filterable. The data layer already
supports it — `repository.observeExercises(ExerciseFilter)` takes `searchText`, `bodyPart`,
`category`, and `includeArchived`. Wire a **search field** + **filter chips** (body part, category,
equipment) into:
- the **exercise picker** used by the live screen and past-workout add (`ui/components/ExercisePicker.kt`), and
- the **Library / Catalog** browse screen (`ui/screens/LibraryScreen.kt`).
Reuse `ui/components/Chips.kt`; debounce the search; keep the minimal-dark look. (This mirrors Lyfta's
searchable exercise list.)

## 4. Exercise images (forward-looking — plan, land if cheap)

Per-exercise **pictures/illustrations** greatly help identifying an exercise (Lyfta shows an
anatomical figure per exercise; see `docs/rework/reference/lyfta/`). Scope it:
- Add an optional image reference to `Exercise` (e.g. `imageUrl: String?` and/or a local
  illustration key) as an **additive, nullable** field (schema bump + `Migration` if you persist it —
  a `2.json`/`3.json` baseline pattern already exists; never destructive fallback). Update
  `ExportBundle` tolerance.
- **Source:** the **wger** sync already runs (`AppContainer.wgerSync`) and wger exposes exercise
  images — populate `imageUrl` during sync where available (respect wger's license/attribution). For
  seeded/custom exercises without an image, fall back to the body-part illustration used by
  `BodyHeatmap` or a category glyph.
- **Render:** a small thumbnail in the exercise picker rows, Library list, and the exercise
  editor/detail. Load images with a lightweight loader (e.g. Coil); cache to disk; graceful
  placeholder. Keep everything offline-tolerant.
If wger images turn out to be heavy/licensing-unclear, **land the schema field + render path with a
placeholder now** and defer real image population — capture the decision in the report.

## Constraints

- Item 1 is a `data/` + `ui/` fix; items 2–4 are mostly `ui/` (+ an additive schema field for 4).
- `domain/` stays Android-free; the repository stays the only Room/file boundary; never enable
  destructive migration fallback.
- Don't regress the merged Phase 0–4 behavior; match the design system.

## Build / verify (this machine)

Canonical SDK `%LOCALAPPDATA%\Android\Sdk` (build-tools **35.0.0**); JDK 17 via
`~/.gradle/gradle.properties`. Stale-daemon "SDK location not found" → `.\gradlew.bat --stop`. Verify
`testDebugUnitTest` + `connectedDebugAndroidTest`, then `.\tools\build-and-install.ps1 -Launch` and
walk the set-done flow + search + navbar on the A56 (`.\tools\phone.ps1 screenshot`;
`adb logcat -b crash -d` must be clean).

## Done when

Set-done no longer crashes (atomic save + serialized persist, with a regression test); the "Start"
label is centered; exercise search/filter works in the picker and Library; exercise-image support is
at least scaffolded (field + render path + placeholder, wger-populated if feasible). Then write
`docs/rework/bugfix-polish-report.md` (what was fixed, the crash root-cause confirmation, and any
image-sourcing decision).
