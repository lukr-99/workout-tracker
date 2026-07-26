# Handoff Prompt — Phase 1: Data core (Room + repository + domain)

> **Status: ✅ COMPLETED & MERGED** on `feature/app-rework` — see [phase-1-report.md](phase-1-report.md).
> Kept for history.

> Paste the block below as the opening prompt for the next agent. It is self-contained.

---

You are building **Phase 1 (the data core)** of the native-Android **Workout Tracker** rewrite.
Phase 0 is done and verified: a Gradle/Compose app that builds, installs on the phone, and shows a
floating-nav shell over empty dark screens. Your job is to make the app **persist real data** — no
new screens required. Work on branch **`feature/app-rework`** in `F:\Code\workout-tracker` (commit +
push there). The frozen `.NET MAUI` app on `release/1.0` / tag `v1.0.0` is **read-only reference**.

## Read first (this is the spec — port it, don't invent)

- `docs/rework/phase-0-report.md` — what exists now, and Section 7 tech-debt you'll act on.
- `docs/rework/01-architecture.md` — the `data/` + `domain/` package layout to fill in.
- `docs/rework/03-data-model.md` — the entity map, additive fields, and export bundle. **Authoritative.**
- The **source of truth to port from** (real MAUI code, do not edit):
  - `WorkoutTracker.Core/Data/Records.cs` — the 7 table shapes + indexes.
  - `WorkoutTracker.Core/Domain/Models.cs` — enums, domain classes, projections, `ExportBundle`.
  - `WorkoutTracker.Core/Seed/SeedExercises.cs` — the 16 seeded exercises.
  - `WorkoutTracker.Core/Data/WorkoutTrackerRepository.cs` — the query/behaviour surface to mirror.
- Mirror the **ring-set** app's `data/` style (Room DAO/Db/Repository + Flow): `lukr-99/ring-set`.

## Scope — what Phase 1 delivers

A working, tested persistence layer wired to the existing repository/VM seams. **No UI work beyond
proving it** (a temporary debug read-out on Home is fine but optional; real screens are Phase 2).

### 1. Room entities (`data/Entities.kt`)
Port the 7 records to `@Entity`, matching `03-data-model.md`:
`exercises, templates, template_exercises, sessions, entries, strength_sets, cardio_data`.
- Ids: `String` GUID PKs. FKs with `onDelete = CASCADE` and indexes matching `Records.cs` `[Indexed]`.
- Enums (`ExerciseCategory`, `ExerciseSource`, `WorkoutSessionStatus`) stored as **Int ordinals**
  with the **exact MAUI values** (Strength=0/Cardio=1, Seeded=0/Synced=1/Custom=2,
  Active=0/Completed=1/Discarded=2) so a `v1.0` JSON export imports 1:1.
- Timestamps → `Long` epoch-millis UTC (MAUI used `DateTime` UTC).
- `decimal` → `Double`. `List<String> secondaryBodyParts` → JSON via a `TypeConverter`.
- **Add the additive fields** from `03-data-model.md §"New fields"** (`strength_sets.isWarmup`,
  `isPr`, `durationSeconds`, `setType: SetType` enum, `entries.supersetGroup`,
  `sessions.perceivedEffort`, `sessions.bodyweightKg`, `exercises.defaultRestSeconds`) as
  nullable/defaulted so they don't break the `1.0` shape. Add the `SetType` enum
  (`Normal, Warmup, Drop, Failure, Negative, BackOff`).

### 2. DAO + read models (`data/WorkoutDao.kt`, `data/Models.kt`)
- Flow-returning queries: catalog (with `ExerciseFilter`: search/body-part/category/includeArchived),
  templates, session history, active session, session detail.
- `@Relation` read models `SessionWithEntries` / `EntryWithSets` (+ cardio) for detail loads.
- Upserts for every entity; archive (not delete) for exercises; cascade deletes for sessions.
- Analytics **projection queries** feeding the `domain/` engines (recent-session summaries,
  per-exercise points) — see the MAUI repository for the exact set to mirror.

### 3. Database (`data/WorkoutDb.kt`)
- `@Database` v1 with all entities + `TypeConverters`. Export the schema
  (`room.schemaLocation` → checked-in JSON under `app/schemas/`). No destructive fallback in release.
- **Switch Room from kapt to KSP** (report §7): add the KSP plugin to the catalog + `app`, replace
  `kapt(libs.room.compiler)` with `ksp(...)`, drop the kapt plugin. Confirm the build is warning-free.
- **Seed on first run** (empty DB): port `SeedExercises.Create()` (16 exercises, `source = Seeded`).

### 4. Repository (`data/WorkoutRepository.kt`)
Replace the Phase-0 stub. The **only** class that touches Room/files. Constructor-inject the DAO.
Expose Flow queries + suspend upserts mirroring `WorkoutTrackerRepository.cs`, including the
history-safe rules from `03-data-model.md`:
- **Snapshot on log**: entries copy `exerciseSnapshotName/Category/PrimaryBodyPart` at add time.
- Template → session instantiation copies template exercises into entries (no live FK to template).
- Editing/archiving a catalog exercise never rewrites past sessions.
- Provide a minimal **`AppContainer`/ServiceLocator** (manual DI, ring-set style) that builds the
  Db → Dao → Repository and hands the repo to `WorkoutViewModel`. Wire it in `MainActivity`.

### 5. domain/ (pure Kotlin, no Android imports)
Port the analytics the MAUI repo computed, as pure functions over the read models:
- `domain/Analytics.kt` — `AnalyticsOverview`, `WorkoutConsistencySnapshot` (7/30-day counts,
  weekly streak, longest gap), total volume, most-logged exercise.
- `domain/Progression.kt` — per-exercise `ExerciseAnalyticsPoint` series (best weight, volume, reps).
- `domain/Estimates.kt` — e1RM (Epley/Brzycki), volume/tonnage. `domain/Units.kt` already exists —
  extend it.
Keep everything here `@Serializable`/primitive-friendly and covered by JVM unit tests.

### 6. Export/import contract (`data/export/`)
- `ExportBundle.kt` — `@Serializable`, mirroring the MAUI `ExportBundle`
  (`exportedAtUtc`, `exportFormatVersion`, `exercises/templates/sessions` nested). Bump the version
  to **`1.1`** now that the additive fields exist; the reader must accept `1.0` **and** `1.1` and
  ignore unknown fields.
- `JsonExporter.kt` (bundle ⇄ JSON) + `BundleImporter.kt` (round-trip/restore). CsvExporter and the
  **Lyfta importer are Phase 3** — leave `importer/` with a TODO stub referencing
  `05-lyfta-import.md`. (Do *not* build the Lyfta importer here.)

## Testing (mirror the MAUI `WorkoutTracker.Tests` coverage)
- **Instrumented Room tests** (`androidTest`): DAO CRUD, `ExerciseFilter`, cascade deletes,
  template→session snapshotting, archive-not-delete, seed-on-empty.
- **JVM unit tests** (`test`): the `domain/` analytics + estimates against handcrafted data.
- **Round-trip test**: build a bundle → JSON → import into a fresh DB → assert equality.
- Add `androidx.test`/`room-testing`/`kotlinx-coroutines-test` to the catalog as needed.

## Constraints & conventions
- `domain/` stays **Android-free** (enforced — it's the portability seam toward a future desktop tool
  that reads the same `ExportBundle`; see `01-architecture.md §Portability seam`). No KMP.
- Repository is the only IO boundary; ViewModels call the repo; screens call the VM.
- Match MAUI enum ordinals and GUID id format **exactly** — cross-device import depends on it.
- Keep commits focused. Tick "Data core" in `docs/rework/README.md`. Verify with
  `.\gradlew.bat testDebugUnitTest` and, for instrumented tests, `.\tools\phone.ps1` /
  `connectedDebugAndroidTest` on the Galaxy A56. Commit + push `feature/app-rework`.

## Done when
Entities/DAO/Db/Repository/domain/export exist and compile warning-free (KSP, not kapt); the DB
seeds on first run; unit + instrumented + round-trip tests are green; the repo is wired through
`AppContainer` into `WorkoutViewModel`. Then write `docs/rework/phase-1-report.md` (same style as
phase-0) covering what shipped, any schema deviations, and what Phase 2 (the logging loop + screens)
should build on top.
