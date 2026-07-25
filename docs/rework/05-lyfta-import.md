# Lyfta Data Import

Goal: get **all** existing training history out of Lyfta (`com.lyfta`) and into the new app,
losslessly where possible.

## Extraction path (decided)

Lyfta is a **release-signed** third-party app, so `adb run-as` and `adb backup` on its private
database won't work without root. The clean, supported path is **Lyfta's own CSV export**:

1. In Lyfta: **Settings → Export data → CSV** (Lyfta added CSV export; it also *imports* from
   other apps, which confirms a documented tabular format).
2. Lyfta writes/shares a CSV — either to `Downloads/` or via the Android share sheet.
3. Pull it to the PC with the tooling:
   ```powershell
   .\tools\pull-lyfta.ps1        # scans Downloads + common export dirs, copies CSV(s) to .\import\lyfta\
   ```
4. Inspect the real columns and finalize `LyftaCsvImporter`.

> If CSV export turns out to be limited (e.g. summaries only, no per-set rows), fall back to:
> a) Lyfta → **Health Connect** sync, then read Health Connect; or
> b) a manual per-exercise export. We confirm which during the extraction session.

## Expected CSV shape (to confirm against a real export)

Set-level workout exports of this kind typically carry roughly:

| Likely column | Maps to |
|---------------|---------|
| Date / datetime | `sessions.startedAtUtc` (grouped into a session per day/workout) |
| Workout / routine name | `sessions.name` |
| Exercise name | `exercises.name` (match existing, else create custom) + `entries.exerciseSnapshotName` |
| Set number | `strength_sets.setNumber` |
| Reps | `strength_sets.reps` |
| Weight (+ unit) | `strength_sets.weightKg` (convert lb→kg if needed) |
| RPE / RIR | `strength_sets.rpe` / `.rir` (if present) |
| Distance / duration / calories | `cardio_data.*` for cardio rows |
| Notes | `entries.notes` / `strength_sets.notes` |
| Body part / category | `exercises.primaryBodyPart` / `.category` when available |

**These are assumptions.** The mapping is finalized only after we see a real file. A checked-in
sample (with any personal data trimmed) becomes the importer test fixture.

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

Blocked on: **USB debugging authorization** on the phone, then an in-app Lyfta CSV export.
Once the real file is in hand, this doc's "expected" table is replaced with the confirmed schema.
