# Pitfalls

Things that went wrong in this repository and took longer to find than to fix. Search here, and in
CodePrint's `docs/pitfalls/`, for the error text before debugging something surprising. The format
is in CodePrint's `docs/pitfalls/README.md`. Newest first.

## A PowerShell 5.1 rewrite turns `—` into `â€”` in Kotlin sources

- Symptom: after a scripted edit, a source file shows `â€”`, `Ã—` or `â†’` where it had `—`, `×`
  or `→`, and `git diff` shows changes on lines the script never meant to touch.
- Cause: Windows PowerShell 5.1 `Get-Content` reads a UTF-8 file without a byte-order mark as the
  ANSI code page (Windows-1252). Writing that text back as UTF-8 encodes the misread bytes again.
- Fix: read with `Get-Content -Encoding UTF8` or `[IO.File]::ReadAllText($p, [Text.UTF8Encoding]::new($false))`,
  or edit with a tool that keeps the encoding. To repair a damaged file, re-encode its text to
  Windows-1252 bytes and decode those as UTF-8, then check `git diff` for leftovers.
- Closed off by: not yet.
- Seen: richer notes work, 2026-10-04, while moving `EntryCard` out of `LiveWorkoutScreen.kt`. The
  old `.gitignore` had the same damage in its comments, so it happened here before too.

## `connectedAndroidTest` uninstalls the app it tested, on every connected device

- Symptom: after an instrumented test run, the app is gone from the phone along with its data.
- Cause: the Gradle connected-test task installs on every attached device and uninstalls the app
  under test when it finishes. Debug builds used the release application id, so a run with the
  phone plugged in would have replaced and then removed the real app.
- Fix: debug builds are now their own app (`com.lukr99.workout.debug`, "Ember dev"). Still pin test
  runs to the emulator with `$env:ANDROID_SERIAL = 'emulator-5554'`.
- Closed off by: `applicationIdSuffix = ".debug"` in `app/build.gradle.kts`.
- Seen: richer notes work, 2026-10-04. Caught before it ran with the phone attached.

## A dropped download handed a partial APK to the installer (2.5.2)

- Symptom: tapping install opened Android's package installer, which failed as if the APK were
  corrupt. Nothing was logged.
- Cause: a dropped connection ends the response stream without an error, so the download "finished"
  with a partial file. Every updater error was also swallowed by `runCatching { }.getOrNull()`.
- Fix: check the HTTP status, compare the byte count with the size GitHub reports, write to a
  `.part` file and move it into place only when complete. Show and log the real failure reason.
- Closed off by: `AppUpdaterDownloadTest` (instrumented).
- Seen: v2.5.2, 2026-09-20, commit 0558b41.

## A PR chip crashed the live workout on every launch (2.5.1)

- Symptom: marking a set done killed the app with `IllegalStateException`, and the relaunched app
  died again within seconds.
- Cause: the exercise card sized its superset rail with `IntrinsicSize.Min`, and the set tag chips
  were a `LazyRow`. A `SubcomposeLayout` cannot answer an intrinsic measurement, so Compose threw.
  `isPr` is saved in the draft, so the same chip came back on every launch.
- Fix: the chip strip is a plain scrolling `Row`, and the rail is painted with `drawBehind`. Do not
  put lazy lists inside anything measured with intrinsics.
- Closed off by: `SetRowLayoutTest` (instrumented Compose test).
- Seen: v2.5.1, 2026-09-20, commit 2564058.

## Haptics broke CI until the vibrator permission was declared

- Symptom: the Android CI build failed after haptic feedback was added. The exact text was not kept.
- Cause: the code used the vibrator without declaring `android.permission.VIBRATE`.
- Fix: declare the permission in `AndroidManifest.xml`.
- Closed off by: `lintDebug` in CI.
- Seen: 2026-08-29, commit 2c076a4.

## Exports and backups silently left out runs and routes

- Symptom: a backup restored strength history but no runs or saved routes. All tests passed.
- Cause: Run Mode added its own tables and repository, but the JSON export, automatic backup and
  import still only knew about strength data.
- Fix: `DataTransferService` owns `RunRepository` and round-trips runs and routes both ways. When a
  feature adds a table, add it to the export bundle and the round-trip test in the same change.
- Closed off by: `WorkoutRepositoryTest.exportImport_roundTripsIntoFreshDatabase`.
- Seen: v2.2.0, 2026-08-10, commit 868afbb.

## GPS arrives in bursts with the screen off

- Symptom: a run recorded with the phone locked draws straight lines across long gaps.
- Cause: without a wake lock the location callbacks and the 1 Hz clock are throttled while the
  screen is off.
- Fix: hold a partial wake lock while a run records (`WAKE_LOCK` permission).
- Closed off by: not yet; only checked on a real run.
- Seen: v2.2.0, 2026-08-10, commit c400e29.

## Android 16 warns that native libraries are not 16 KB aligned

- Symptom: Android shows a 16 KB page size compatibility warning when the app starts.
- Cause: MapLibre 11.5.2, DataStore 1.1.1 and a transitive `androidx.graphics:graphics-path` shipped
  `.so` files aligned to 4 KB.
- Fix: bump MapLibre and DataStore and pin `graphics-path` 1.0.1. Each `.so` must report
  `LOAD p_align=16384`.
- Closed off by: not yet; check the APK after native dependency bumps.
- Seen: v2.1.1, 2026-08-08, commit e0e2e97.

## The first release-signed install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

- Symptom: `adb install` or the updater fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- Cause: Android only upgrades in place when the new APK has the same signing key. A debug-signed
  copy cannot be upgraded by a release-signed one.
- Fix: export the data, uninstall, install the release build, and import. Keep the release keystore
  backed up, because losing it forces this on every future update (`docs/RELEASING.md`).
- Closed off by: debug builds now install as a separate app, so they never block a release install.
- Seen: first release install, 2026-08-22.

## The old Visual Studio `.gitignore` swallowed Android source folders

- Symptom: new files under `app/src/debug/` or a `data/backup/` package did not show up in
  `git status`.
- Cause: the MAUI-era `.gitignore` ignored `[Dd]ebug/` and `Backup*/`, which also matched source
  folders.
- Fix: replaced with the CodePrint `.gitignore`.
- Closed off by: check `git ls-files -ci --exclude-standard` is empty after editing `.gitignore`.
- Seen: Run Mode and backup work, 2026-08; removed 2026-10-04.
