# Lyfta Data Import

Goal: get **all** existing training history out of Lyfta (`com.lyfta`) and into the new app,
losslessly where possible.

## Extraction path (done)

Lyfta is a **release-signed** third-party app, so `adb run-as` and `adb backup` on its private
database don't work without root. The supported path is **Lyfta's own CSV export**, and it worked:

1. In Lyfta: export data → CSV. (It shares the file via the Android share sheet / Phone Link.)
2. The file arrives as `export<digits>.csv`. Copy it to `import/lyfta/` (git-ignored), or pull
   from the phone with `.\tools\pull-lyfta.ps1` if it's saved to Downloads.
3. **Confirmed** — a real export was captured and analysed (below).

## Confirmed CSV schema (from a real export, 2026-07-25)

Header (note the leading space before `Title`):

```
 Title,Date,Duration,Exercise,"Superset id",Weight,Reps,Distance,Time,"Set Type"
```

One row **per set**. Sample:

```
"Morning Workout alone Brno, sick","2026-07-13 07:30:19",01:07:22,"Bench Press",,55.000,10,null,null,NORMAL_SET
```

Real export stats: **468 set rows · 19 sessions · 42 exercises · 2026-03-18 → 2026-07-13.**

| Column | Meaning | Maps to |
|--------|---------|---------|
| `Title` | Session name (shared by all rows of a workout) | `sessions.name` |
| `Date` | Session start `yyyy-MM-dd HH:mm:ss` (shared; groups rows into a session) | `sessions.startedAtUtc` |
| `Duration` | Session length `HH:mm:ss` (shared) | `sessions.durationSeconds` |
| `Exercise` | Exercise name (e.g. "Lever Narrow Grip Seated Row") | catalog match/create + `entries.exerciseSnapshotName` |
| `Superset id` | Superset group — **empty in this dataset** | `entries.supersetGroup` (when present) |
| `Weight` | kg, `NN.000`; `0.000` = bodyweight; `null` = timed | `strength_sets.weightKg` (0 for bodyweight; null→0) |
| `Reps` | integer; empty for timed holds | `strength_sets.reps` |
| `Distance` | `null`/empty — **unused in this dataset** | `cardio_data.distanceKm` (if ever present) |
| `Time` | `M:SS` hold time for timed/isometric sets (planks, stairs) | `strength_sets.durationSeconds` (new field) |
| `Set Type` | `NORMAL_SET` / `WARMUP_SET` / `DROP_SET` / `NEGATIVE_REPS_SET` / `FAILURE_SET` / `BACK_OFF_SET` | `strength_sets.setType` (new enum) + `isWarmup` |

Set-type distribution in the real file: NORMAL 418 · WARMUP 26 · DROP 14 · NEGATIVE 5 ·
FAILURE 4 · BACK_OFF 1. This is what motivates the `SetType` enum and `durationSeconds` field
added in [03-data-model.md](03-data-model.md).

The full personal export lives at `import/lyfta/lyfta-export.csv` (git-ignored). In Phase 3 a
small **anonymised** slice (fake titles, a few exercises) gets committed as the importer test
fixture under the app's `src/test/resources/` — the raw personal file itself is never committed.

## Import algorithm (`data/importer/LyftaCsvImporter.kt`)

1. **Parse** the CSV (kotlin-csv or manual), tolerant of column-order and header naming variants.
2. **Group rows into sessions**: consecutive rows sharing date (+ workout name) become one
   `WorkoutSession`; distinct exercises within → `WorkoutEntry`; rows → `StrengthSet` /
   `CardioEntryData`.
3. **Resolve exercises**: fuzzy-match names against the seeded catalog (case/whitespace-insensitive,
   simple alias table). No match → create a `Custom` exercise; always snapshot onto the entry.
4. **Units**: detect kg/lb from a unit column or a user prompt; normalize to kg.
5. **Dedupe**: skip a session if one already exists with the same date + name + set signature
   (idempotent re-imports). Report imported / skipped / created-exercises counts.
6. **Preview then commit**: show a summary (N sessions, N exercises, date range) before writing,
   inside a single Room transaction.

## Mapping edge cases

- Cardio-only rows (duration/distance, no reps) → cardio entry.
- Bodyweight exercises (weight = 0 / blank) → kept as strength sets with `weightKg = 0`.
- Missing set numbers → inferred by row order within an exercise.
- Timezones → treat Lyfta timestamps as local; store UTC.
- Duplicate exercise names differing only by equipment → alias table maps to one catalog entry.

## Deliverables for this workstream

- `tools/pull-lyfta.ps1` — pull Lyfta's exported CSV(s) off the phone. *(built now)*
- A trimmed sample CSV under `docs/rework/reference/lyfta-sample.csv`. *(after extraction)*
- `LyftaCsvImporter` + unit tests against the sample. *(Phase 3)*
- An in-app **Import from Lyfta** flow (pick file → preview → commit). *(Phase 3)*

## Status

- [x] USB debugging authorized; phone reachable via `tools/`.
- [x] Lyfta CSV exported and captured (`import/lyfta/lyfta-export.csv`, 468 sets / 19 sessions).
- [x] Schema confirmed and mapped (above); schema fields folded into the data model.
- [ ] `LyftaCsvImporter` implemented + tested against a fixture slice (Phase 3).
- [ ] In-app "Import from Lyfta" flow (Phase 3).

Nothing further is needed from the phone for Lyfta — the full history is already on the PC. The
importer is a pure code task in Phase 3, testable offline against the captured file.
