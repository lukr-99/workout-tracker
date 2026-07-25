# Phase 1 Report — Data core (Room + repository + domain)

> **For:** the Phase 2 agent (the logging loop + real screens), and the design agent reviewing.
> **From:** the Phase 1 data-core agent.
> **Branch:** `feature/app-rework` · **Date:** 2026-07-25
> **Spec followed:** `docs/rework/01-architecture.md`, `03-data-model.md`, porting from the frozen
> MAUI `WorkoutTracker.Core` (`release/1.0` / tag `v1.0.0`, read-only reference).

---

## 1. TL;DR

Phase 1 is **complete and verified on real hardware**. The app now **persists real data**: a Room
database seeded with the 16 starter exercises on first run, a repository that mirrors the MAUI
behaviour surface, a pure `domain/` analytics layer, and a versioned JSON export/import contract.
No new screens (that's Phase 2) — but the data core is wired end-to-end through `AppContainer` into
`WorkoutViewModel`, which exposes live Room `Flow`s.

- **`.\gradlew.bat testDebugUnitTest`** → green (domain analytics/estimates/progression + JSON
  round-trip and a hand-crafted `v1.0` import).
- **`.\gradlew.bat connectedDebugAndroidTest`** → **10/10 green** on the Galaxy A56 (DAO CRUD,
  `ExerciseFilter`, cascade deletes, template→session snapshotting, archive-not-delete,
  seed-on-empty, export→JSON→import round-trip into a fresh DB).
- **Installed + launched** on-device; `workout.db` is created and seeded (verified on the phone).
- **Room compiled via KSP** (kapt dropped); schema **exported and checked in** at
  `app/schemas/com.lukr99.workout.data.WorkoutDb/1.json`. Build is warning-free bar the pre-existing
  AGP-vs-compileSdk-35 note (phase-0 §7).

---

## 2. What shipped (against the handoff's 6 scope items)

| # | Handoff scope | Status | Where |
|---|---------------|--------|-------|
| 1 | Room entities (7) + enums + `SetType` + additive fields | ✅ | `data/Entities.kt`, `domain/Enums.kt` |
| 2 | DAO + `@Relation` read models + projections | ✅ | `data/WorkoutDao.kt`, `data/Models.kt` |
| 3 | `@Database` v1, schema export, **kapt→KSP**, seed-on-first-run | ✅ | `data/WorkoutDb.kt`, `data/Seed.kt`, `app/schemas/` |
| 4 | Repository (only IO boundary) + `AppContainer` DI, wired to VM | ✅ | `data/WorkoutRepository.kt`, `data/AppContainer.kt`, `WorkoutApp.kt` |
| 5 | `domain/` analytics/progression/estimates (Android-free) | ✅ | `domain/Analytics.kt`, `Progression.kt`, `Estimates.kt`, `Units.kt` |
| 6 | Export/import contract (bundle ⇄ JSON), Lyfta stub | ✅ | `data/export/`, `data/importer/` |

Testing (handoff §"Testing"): instrumented Room tests + JVM domain tests + the round-trip test are
all present and green. Catalog test deps added: `room-testing`, `androidx.test.*`,
`kotlinx-coroutines-test`.

---

## 3. Architecture as built — the two-model seam

The single most important structural decision: **`domain/` holds the portable, `@Serializable`
model; `data/` holds the Room `@Entity` shapes; the repository maps between them.** This is faithful
to MAUI (where `Domain/Models.cs` *was* the serialized export) and keeps the portability seam clean.

```
ui/ (WorkoutViewModel: StateFlow)          ← collects repo Flows
        │
data/WorkoutRepository  ← the ONLY class touching Room + files
   ├─ maps  Entity ⇄ domain model
   ├─ Room:  Entities.kt · WorkoutDao.kt · Models.kt (@Relation) · WorkoutDb.kt · Converters.kt
   └─ export/ (ExportBundle references domain models) · importer/
        │
domain/ (pure Kotlin, no Android imports)  ← Models · Enums · Analytics · Progression · Estimates · Units
```

- **Why domain models carry serialization annotations:** the `ExportBundle` (`data/export`) is just
  `exercises/templates/sessions: List<domain model>`. The domain models are the wire contract, so
  their fields carry the ordinal-enum and ISO-timestamp serializers (below). `data/` entities stay
  free of serialization concerns; the repository maps.
- **`domain/` is Android-free** (uses only `kotlinx.serialization` + `java.time`), so it stays
  liftable toward a future desktop tool.

### Wire-format fidelity (so a `v1.0` export imports 1:1)

The MAUI export used `System.Text.Json` (Web defaults), which writes **enums as int ordinals** and
**`DateTime` as ISO-8601 strings**. Reproduced in `domain/Serialization.kt`:

- `OrdinalEnumSerializer` — every enum encodes/decodes as its **ordinal Int** (never the name).
  Enum member order is now a **frozen contract** (documented in `Enums.kt`).
- `InstantMillisSerializer` — timestamps are epoch-millis `Long` in memory / Room, but ISO-8601
  strings on the wire (tolerant reader: `Z`, offsets, or bare millis).
- `JsonExporter` sets `ignoreUnknownKeys = true`, so it tolerates the MAUI computed properties
  (`bodyPartsSummary`, `isStrength`) present in real `1.0` files and any future additive fields.

A JVM test decodes a **hand-crafted `v1.0` JSON** (int enums, ISO datetimes, the computed fields)
and asserts it maps 1:1, with the new additive fields defaulting.

---

## 4. Schema deviations & decisions (vs. the MAUI records)

None are breaking; flagging the judgment calls:

1. **Table/column names preserved**, but the additive columns from `03-data-model.md` are live now:
   `strength_sets.isWarmup/isPr/durationSeconds/setType`, `entries.supersetGroup`,
   `sessions.perceivedEffort/bodyweightKg`, `exercises.defaultRestSeconds`. All nullable/defaulted,
   so the `1.0` shape is a strict subset. `ExportBundle.exportFormatVersion` is now **`1.1`**; the
   reader accepts `1.0` **and** `1.1`.
2. **Exercise references are deliberately NOT foreign keys.** `template_exercises.exerciseId` and
   `entries.exerciseId` have no FK — history is immutable-by-snapshot and catalog exercises are
   archived, never deleted, so a `CASCADE` there would be wrong. Only template/session/entry
   **children** cascade. (MAUI had no such FKs either.)
3. **Seeding lives in the repository** (`ensureSeeded`, called from `WorkoutApp` on an IO scope),
   not a Room callback — mirrors MAUI `InitializeAsync`, and is directly unit-testable.
4. **Filtering is in-memory in the repository** (`applyFilter`), exactly as MAUI did, because the
   catalog filter is case-insensitive multi-field + secondary-body-part JSON matching that SQL
   `LIKE` can't express cleanly. Catalog size is tiny; this is not a perf concern.
5. **e1RM is new** (`Estimates.kt`, Epley + Brzycki) — MAUI stored raw sets only. Warmups are
   excluded from best-e1RM and can be excluded from volume/reps.
6. **No `MigrationTestHelper` test yet** — there is only one schema version, so there is nothing to
   migrate. The schema **is** exported and checked in, so the first real migration (Phase 2+ when a
   column changes) has its `1.json` baseline ready. **No destructive fallback** is configured — a
   schema change without a migration will (correctly) throw rather than wipe history.

---

## 5. The behaviour surface the ViewModel/screens get (Phase 2 starts here)

`WorkoutRepository` exposes (all mapped to domain models):

- **Catalog:** `observeExercises(filter)` Flow, `getExercise`, `saveExercise` (normalizes like
  MAUI), `archiveExercise`, `mergeExternalExercises`, and `newEntryForExercise(...)` — the
  **snapshot-on-log** helper Phase 2 should use when adding an exercise to a live session.
- **Templates:** `observeTemplates` Flow, `getTemplate`, `saveTemplate` (replace-children),
  `deleteTemplate`.
- **Sessions:** `createWorkoutSession(templateId?, name?)` (instantiates from a template, copying
  snapshots; returns the existing active session if one exists), `observeActiveSession`,
  `observeSession(id)`, `saveWorkoutSession` (sets completion timestamps + duration, replaces
  children with reindexed sort/set numbers), `deleteWorkoutSession` (cascade).
- **History + analytics:** `observeHistory(search?)` Flow of summaries, `getDashboardSnapshot`,
  `getAnalyticsOverview`, `getConsistencySnapshot`, `getExerciseProgress(id)`,
  `duplicateWorkoutAsTemplate`.
- **Backup:** `createExportBundle` / `importBundle`; `BundleImporter.importJson(text)` validates the
  version and applies it.

`WorkoutViewModel` currently surfaces `exercises`, `templates`, `activeSession`, and `history` as
`StateFlow`s (via `stateIn(WhileSubscribed)`), wired from `AppContainer.repository` in
`MainActivity`. Phase 2 should split per-area VMs and add the logging **actions** (add set, edit
reps/kg, complete session) on top of these seams — the repository already supports them.

---

## 6. Build/tooling changes

- `gradle/libs.versions.toml`: added `ksp` plugin (`2.0.21-1.0.28`, matches Kotlin 2.0.21),
  `room-testing`, `androidx.test.{core,runner,ext-junit}`, `kotlinx-coroutines-test`. Removed the
  `kotlin-kapt` plugin.
- `app/build.gradle.kts`: `kapt(room.compiler)` → `ksp(room.compiler)`; `room.schemaLocation` →
  `$projectDir/schemas`; the schema dir is added to `androidTest` assets for future migration tests.
- Root `build.gradle.kts`: `kotlin.kapt` alias → `ksp`.
- Resolves phase-0 §7 tech-debt item #1 (kapt language-version-1.9 warning) — gone with KSP.

---

## 7. Verification commands (reproducible)

```bash
.\gradlew.bat testDebugUnitTest          # JVM: domain + JSON round-trip / v1.0 read
.\gradlew.bat connectedDebugAndroidTest  # on-device: 10 Room tests (Galaxy A56)
.\gradlew.bat assembleDebug              # warning-free (bar the AGP/compileSdk-35 note)
```

Then `tools\build-and-install.ps1 -Launch` installs + launches; `workout.db` is created and seeded
on first run.

---

## 8. What Phase 2 should build on top

- **The logging loop UI** over the existing repo seams: Workout (live session), Templates + editor,
  Catalog + exercise editor, History + detail, Stats. Use `newEntryForExercise` for snapshotting.
- **Per-area ViewModels** + the write actions (the repo already exposes the suspend upserts).
- **Settings via DataStore** (`settings/SettingsStore.kt` still a stub) — units (`domain/Units` is
  ready), theme, rest-timer defaults; the `defaultRestSeconds` column exists for it.
- **Charts** off `getExerciseProgress` / `getDashboardSnapshot`.
- Nothing in the data core should need to change for Phase 2; if a column must change, add a Room
  `Migration` (the `1.json` baseline is checked in) — do **not** enable destructive fallback.
