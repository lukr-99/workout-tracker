# Exercise images report

## Sources and resolution

`ExerciseImageResolver` is the single thumbnail policy:

1. app-private user photo (`localImagePath`), when the file still exists;
2. persisted wger `imageUrl` and attribution;
3. the bundled `free-exercise-db` metadata index;
4. the existing body-part monogram when none of the above resolves.

The open index is generated from
[`yuhonas/free-exercise-db`](https://github.com/yuhonas/free-exercise-db) (Unlicense/public domain).
Only 873 normalized metadata entries are bundled (about 120 KB). Image files remain on GitHub and
Coil supplies its normal memory/disk cache, so viewed images remain available from cache without
adding the full image set to the APK. Run `tools/generate-free-exercise-index.ps1` to refresh the
checked-in asset.

Name matching lowercases, removes accents/punctuation, collapses whitespace, removes a conservative
equipment suffix set, and applies aliases for known catalog differences. The same normalizer drives
the open index and the wger backfill.

## Coverage

- Before this work, a fresh seeded catalog had persisted artwork for 0 of 16 rows.
- The bundled index resolves all 16 seeded exercise names without a network sync.
- On the A56 smoke-test database, a full wger sync reported: 821 added, 0 updated, 5 existing images
  backfilled, and 7 skipped.
- Backfill changes only blank `imageUrl`/`imageAttribution` fields. It does not rename, unarchive, or
  replace notes, equipment, defaults, or personal photos.

## Personal photos and custom exercises

The exercise editor can launch the system camera through a temporary FileProvider URI or the system
photo picker without storage permission. A selected/captured image is copied into
`filesDir/exercise_images/<exerciseId>.jpg`, previews immediately, can be removed, and takes priority
in catalog rows, pickers, and the editor/detail view.

Custom exercise creation is available from the Catalog add action and from empty Catalog/picker
search results as `Create "<query>"`. The editor supports a machine-specific name, category, primary
body part, equipment, and photo.

Room schema v4 adds nullable `localImagePath` through non-destructive `MIGRATION_3_4`; checked-in
`4.json` and migration coverage include both `1 -> 2 -> 3 -> 4` and direct `3 -> 4`. Export format
1.4 includes the path and readers still accept 1.0 through 1.4.

Important: personal photo bytes are device-local. The export contains `localImagePath` for contract
compatibility, but does not embed or copy the file. On another device that path will normally not
exist, and the resolver safely falls through to wger, the open index, or the placeholder.

## Verification

- `testDebugUnitTest`: passed.
- `assembleDebug`: passed.
- `connectedDebugAndroidTest`: 20 tests passed on the A56, including migration and repository
  backfill coverage.
- A56 smoke test: open-dataset thumbnails rendered in Catalog, photo picker opened without a runtime
  permission, selected photo copied and rendered, camera launched through FileProvider and cancel
  preserved the existing photo, custom exercise rendered its photo in Catalog, and wger sync
  reported its backfill count.
