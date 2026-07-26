# Handoff Prompt — Exercise images (wger backfill + open dataset + personal photos)

> Paste the block below as the opening prompt for Codex. Self-contained. Touches **`data/` + `ui/`**.
> This is the last feature before the **final cutover** ([final-phase-cutover.md](final-phase-cutover.md)),
> so land it clean on `feature/app-rework` (or a short `feature/exercise-images` branch to merge back).

---

You are adding **rich exercise imagery** to the native-Android **Workout Tracker** (Phases 0–5 done on
`feature/app-rework`, `F:\Code\workout-tracker`). Exercises already have optional `imageUrl` +
`imageAttribution` (schema v3) rendered as Coil thumbnails with a body-part monogram placeholder; the
wger sync populates images for *freshly synced* rows only. Goal: **much better image coverage** via
three stacked sources, plus **user photos** and easy **custom-exercise creation** (e.g. a specific
gym machine). The frozen MAUI app on `release/1.0` is read-only reference; match the design system
(`docs/rework/02-design-system.md`) and reuse existing components.

## Resolution order (build one `ExerciseImageResolver`)

For any exercise, resolve the thumbnail in this priority, first hit wins:
1. **User photo** — `localImagePath` (below).
2. **wger image** — stored `imageUrl` (+ attribution).
3. **Open dataset** — `yuhonas/free-exercise-db` match by normalized name (below).
4. **Placeholder** — the existing body-part monogram.

Normalization for matching (2–3): lowercase, trim, collapse whitespace, strip punctuation, drop a
small stopword/equipment-suffix set; keep an alias map for known mismatches. Reuse the same
normalization the importer/wger matcher already uses if present.

## 1. wger backfill (data/, `WgerSyncService`)

Add a **backfill pass** that matches **all existing exercises** (seeded + custom + Lyfta-imported —
many currently have no image) to the wger dataset by normalized name and fills `imageUrl` /
`imageAttribution` **only when blank**. Never touch user-owned fields (name/notes/archive). Expose it
so Settings can trigger "Fill missing exercise images" (or fold into the existing wger Sync action and
report a count). Keep OkHttp/network in `data/`.

## 2. Bundled open image dataset (data/, offline-first)

Integrate **`yuhonas/free-exercise-db`** (permissive/public-domain data; images sourced from
Everkinetic — record a generic attribution like `free-exercise-db`):
- **Bundle only the metadata index** in `app/src/main/assets/` (a slimmed JSON: normalized name →
  image relative paths + optional muscle/equipment). Do **not** bundle ~1600 images (APK bloat).
- Resolve image URLs from the raw base
  `https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/<path>` and load via
  **Coil with disk cache** (offline after first view). Provide a `data/images/FreeExerciseImageIndex`
  loader (parse asset once, cache in memory).
- Use it as source #3 in the resolver, and optionally to enrich a blank `primaryBodyPart`/`equipment`
  on custom/imported exercises (additive, only-when-blank).

## 3. Personal photos + custom-exercise creation (ui/ + a schema field)

- **Schema:** add `Exercise.localImagePath: String?` (and `ExerciseEntity`), **additive/nullable** →
  Room **schema v4** with a non-destructive `MIGRATION_3_4` (checked-in `4.json`; extend
  `WorkoutMigrationTest` to cover `1→2→3→4`). Bump `ExportBundle` to **1.4** (readers still accept
  1.0–1.4, ignore unknown). Note in the report that local photos are device-local (the file isn't in
  the export bundle) — the path/field exports but the image stays on the device.
- **Photo capture/pick in `ExerciseEditorScreen`:** an image section with **Take photo**
  (`ActivityResultContracts.TakePicture` via the existing `FileProvider`) and **Choose photo**
  (`ActivityResultContracts.PickVisualMedia` — no runtime permission needed). Copy the chosen image
  into app-private storage (e.g. `filesDir/exercise_images/<exerciseId>.jpg`), set `localImagePath`,
  show a preview, allow remove. Render it (priority #1) in the picker, Library/Catalog rows, and
  exercise detail.
- **Custom exercise for a specific gym/machine:** make **"Create exercise"** easy to reach from the
  Library/Catalog and from the exercise picker's empty/no-results state ("Create '<query>'"). The
  editor should let the user name it (e.g. "Hammer Strength Iso Chest Press — MyGym"), set body
  part/category/equipment, and attach their own photo of that machine. (Custom exercises are already
  first-class; this is UX + the photo field.)

## Constraints

- `domain/` stays Android-free; the repository is the only Room/file boundary; images/network/Android
  types live in `data/`/`ui/`. Consume existing seams — don't duplicate sync/import/stats logic.
- Migrations are additive and non-destructive (never `fallbackToDestructive`); keep the checked-in
  schema baselines.
- Match the minimal-dark design system; reuse `ui/components/*` (Coil thumbnail treatment already
  exists from the bug-fix phase).
- Coil is already a dependency. Only add what's needed (e.g. `activity-compose` contracts already
  present).

## Build / verify (this machine)

Canonical SDK `%LOCALAPPDATA%\Android\Sdk` (build-tools **35.0.0**); JDK 17 via
`~/.gradle/gradle.properties`. Stale-daemon "SDK location not found" → `.\gradlew.bat --stop`. Verify
`testDebugUnitTest` + `connectedDebugAndroidTest` (incl. the `3→4` migration), then
`.\tools\build-and-install.ps1 -Launch` and walk it on the A56: open the Catalog/picker (many
exercises now show images), backfill missing images, create a custom "gym machine" exercise with a
gallery/camera photo, and confirm the photo shows in the picker + detail (`.\tools\phone.ps1
screenshot`).

## Done when

The resolver renders the best available image (user photo → wger → free-exercise-db → placeholder);
the wger backfill fills missing images for seeded/imported exercises; the dataset index is bundled and
images load+cache; users can attach a personal photo and easily create a custom machine-specific
exercise; migrations `3→4` + `ExportBundle` 1.4 are in with tests. Then write
`docs/rework/exercise-images-report.md` (sources wired, coverage before/after, the local-photo/export
caveat) — after which we run the final cutover.
