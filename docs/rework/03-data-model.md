# Data Model

Ported from the MAUI `WorkoutTracker.Core/Domain/Models.cs` (the `v1.0.0` spec) to Room. Shapes
are preserved so a `v1.0.0` JSON export imports cleanly; ids stay string GUIDs for stable
cross-device identity.

## Entity map (MAUI → Room)

| MAUI class | Room `@Entity` | Notes |
|-----------|----------------|-------|
| `Exercise` | `exercises` | `secondaryBodyParts: List<String>` via a JSON `TypeConverter`. `category`, `source` as enum ints. |
| `WorkoutTemplate` | `templates` | |
| `WorkoutTemplateExercise` | `template_exercises` | FK → `templates.id`, `sortOrder`. |
| `WorkoutSession` | `sessions` | `status`, timestamps as epoch-millis `Long` (UTC). |
| `WorkoutEntry` | `entries` | FK → `sessions.id`. Holds **snapshot** exercise fields. |
| `StrengthSet` | `strength_sets` | FK → `entries.id`. `weightKg` as `Double` (was `decimal`). |
| `CardioEntryData` | `cardio_data` | 1:1 with a cardio `entry` (FK, unique). |

Projection/analytics types (`WorkoutSessionSummary`, `AnalyticsOverview`,
`WorkoutConsistencySnapshot`, `ExerciseAnalyticsPoint`, `DashboardSnapshot`) are **not** tables —
they're computed by DAO queries + `domain/` functions, same as MAUI.

## Relationships

```
exercises ──(catalog, referenced by id + snapshotted)──┐
                                                        │
templates 1───* template_exercises ──(ref exercise)────┤
                                                        │
sessions  1───* entries 1───* strength_sets            │
                     │                                  │
                     1───0..1 cardio_data              (snapshot on entry)
```

- Room `@Relation` powers `SessionWithEntries` / `EntryWithSets` read models.
- All child deletes cascade (`onDelete = CASCADE`).

## Rules carried over from MAUI (`docs/decisions.md`)

- **History is immutable by snapshot.** `entries` copy `exerciseSnapshotName`,
  `exerciseSnapshotCategory`, `exerciseSnapshotPrimaryBodyPart` at log time. Editing or archiving
  a catalog `exercise` never rewrites past sessions.
- **Template edits don't mutate old sessions.**
- **Deletion = archive** for exercises (`isArchived`), so history stays intact.
- **Store raw, derive stats.** No summary tables; analytics come from queries over history.

## Types & converters

- `decimal` (C#) → `Double` (Kotlin/Room). Weights/volume are display-rounded in `domain/Units`,
  not stored rounded.
- Enums (`ExerciseCategory`, `ExerciseSource`, `WorkoutSessionStatus`) → stored as `Int`
  ordinals, matching the MAUI enum values exactly (Strength=0/Cardio=1, etc.) so JSON imports map
  1:1.
- Timestamps → `Long` epoch-millis UTC. (MAUI used `DateTime` UTC.) Converters keep display in
  local time.
- `List<String>` (secondary body parts) → JSON string via a kotlinx.serialization `TypeConverter`.
- Ids → `String` (32-char GUID "N" format), generated with `UUID.randomUUID().toString()...`.

## New fields the rework adds (additive, nullable/defaulted)

These support roadmap features without breaking the `v1.0.0` shape:

- `strength_sets.isWarmup: Boolean = false`
- `strength_sets.isPr: Boolean = false` (denormalized flag; source of truth is derived)
- `strength_sets.durationSeconds: Int? = null` (timed/isometric sets — planks, holds; absorbs
  Lyfta's `Time` column)
- `strength_sets.setType: SetType = Normal` (enum: `Normal, Warmup, Drop, Failure, Negative,
  BackOff` — a superset of Lyfta's `Set Type`; `Warmup` also sets `isWarmup`)
- `entries.supersetGroup: Int? = null` (group entries into supersets; Lyfta `Superset id`)
- `sessions.perceivedEffort: Int? = null` (session RPE)
- `sessions.bodyweightKg: Double? = null` (optional log-time bodyweight)
- `exercises.defaultRestSeconds: Int? = null`

The `ExportBundle` version bumps to `1.1` when these ship; the importer reads `1.0` and `1.1`.

## Migrations

- **Room schema versions** are checked in (`room.schemaLocation`) and migrated explicitly; no
  destructive fallback in release builds.
- **Seed** runs once on empty DB (ported `SeedExercises.cs`), tagged `source = Seeded`.
- **Data-in migration** (from Lyfta / MAUI) is *import*, not Room migration — handled by
  `data/importer/` (see [05-lyfta-import.md](05-lyfta-import.md)).

## Export bundle (cross-device contract)

`@Serializable ExportBundle` mirrors the MAUI `ExportBundle`:

```
{
  "exportedAtUtc": "<ISO-8601>",
  "exportFormatVersion": "1.1",
  "exercises": [ ... ],
  "templates": [ ... ],
  "sessions": [ { ..., "entries": [ { ..., "strengthSets": [...], "cardioData": {...} } ] } ]
}
```

This is the single format any future desktop/Windows tool reads and writes. Importers must
tolerate older `exportFormatVersion` values and ignore unknown fields.
