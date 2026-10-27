# Pitfalls

Things that went wrong in this repository and took longer to find than to fix. Search here, and in
CodePrint's `docs/pitfalls/`, for the error text before debugging something surprising. The format
is in CodePrint's `docs/pitfalls/README.md`. Newest first.

## Playwright loses a file chooser opened right after `waitForEvent`

- Symptom: a browser check times out waiting for `filechooser` after a click that really called
  `input.click()` (a hook on `HTMLInputElement.click` shows it). It is flaky, and CI hit it every time.
- Cause: `Promise.all([page.waitForEvent("filechooser"), click])` can click before Chromium has
  switched file chooser interception on, so the native chooser opens and the event is lost.
- Fix: register `page.on("filechooser", ...)` once, push the choosers to a queue and poll it.
  See `checkExercisePhotos` in `tools/check-web-dist.cjs`.
- Closed off by: the check uses the queue for every chooser; copy that pattern for new ones.
- Seen: web edition photo check, 2026-10-27.

## The web edition wrote `.e-7` into JSON, and its build ran out of memory

- Symptom: a document or backup written by the web edition did not parse (`Unexpected JSON token`)
  when a number was exactly `1e-7`. Separately, linking the web build failed with "Not enough memory".
- Cause: Kotlin/Wasm 2.0.21 prints some doubles in exponent form without a leading digit (`.e-7`),
  which kotlinx.serialization copies into the JSON. Linking Kotlin/Wasm needs more heap than the old
  2 GB `org.gradle.jvmargs`.
- Fix: `data/document/JsonNumberRepair.kt` repairs such numbers in the web document codec and in
  `JsonExporter.toJson`. `gradle.properties` gives Gradle 3 GB and the Kotlin daemon 4 GB.
- Closed off by: `JsonNumberRepairTest`.
- Seen: web edition M1, 2026-10-26.

## A new `Exercise` field is reset whenever the exercise editor saves

- Symptom: a flag such as `trackPerLocation` is set, then turns off again after the exercise is
  edited and saved for an unrelated reason, like a new note.
- Cause: the editor saves through `ExerciseDraft` and `WorkoutFactory.exercise`, which build a new
  `Exercise` from the draft's fields. A field the draft does not carry falls back to its default.
- Fix: add the field to `ExerciseDraft`, pass it in `WorkoutFactory`, and fill it from the loaded
  exercise in `ExerciseEditorScreen`.
- Closed off by: `ExerciseLocationCreationTest`.
- Seen: training locations, 2026-10-22, caught in review before it shipped.

## The update check failed with `NetworkOnMainThreadException` (2.6.0)

- Symptom: Settings > Updates > "Check now" showed "Check failed: NetworkOnMainThreadException".
  The launch check failed silently, and a download would have failed the same way.
- Cause: the old `AppUpdater` wrapped its work in `withContext(Dispatchers.IO)`. The refactor into
  `UpdateService` (74c2227) kept the functions `suspend` but dropped the dispatcher switch, and
  `viewModelScope` runs on Main. A `suspend` function is not off the main thread by itself. The JVM
  tests used fakes and never hit Android's StrictMode network check, so nothing caught it.
- Fix: `UpdateService.check()` and `download()` switch to an injected IO dispatcher themselves, so
  no caller can get it wrong.
- Closed off by: `UpdateServiceTest.networkCallsLeaveTheCallingThread`, which checks that every
  network call runs on the injected dispatcher.
- Seen: v2.6.0 on the phone, 2026-10-14. A broken updater cannot ship its own fix: the fixed
  version has to be installed by hand once.

## A Robolectric screenshot test fails with "uncaught exceptions before the test started"

- Symptom: a Compose screenshot test fails at random with
  `UncaughtExceptionsBeforeTest`, caused by
  `IllegalStateException: Illegal connection pointer ... arch_disk_io`.
- Cause: Robolectric starts the real `WorkoutApp` for every test. Its `onCreate` builds the Room
  database and seeds the catalog on a background thread, and that work outlives the test, then
  fails while the next test runs.
- Fix: run rendering tests with `@Config(application = android.app.Application::class)`.
- Closed off by: `ComponentScreenshotTest` uses it; AGENTS.md says to.
- Seen: CodePrint 4/5 (agent loop), 2026-10-05.

## Merging the bottom of a PR stack with `--delete-branch` closes the PR above it

- Symptom: after `gh pr merge 3 --merge --delete-branch`, the PR stacked on it shows as closed, and
  `gh pr edit 4 --base main` fails with `Cannot change the base branch of a closed pull request`.
- Cause: deleting a PR's base branch makes GitHub close that PR rather than retarget it. `gh pr
  merge --delete-branch` also deletes the local branch.
- Fix: push the base branch back from the merged PR's `headRefOid`
  (`git push origin <sha>:refs/heads/<branch>`), then `gh pr reopen`, `gh pr edit --base main`, and
  delete the branch again. Next time, retarget the upper PR to `main` before merging the lower one.
- Closed off by: not yet; a habit.
- Seen: PRs lukr-99/workout-tracker#3 and #4, 2026-10-05. Reported to CodePrint.

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
- Closed off by: `HttpsUpdateHttpTest` (instrumented) and `UpdateServiceTest`. Since CodePrint 3/5
  the SHA-256 check would also catch it.
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

## A full-screen Compose `Dialog` still sits between the system bars

- Symptom: a `Dialog` with `usePlatformDefaultWidth = false` and `decorFitsSystemWindows = false`
  starts below the status bar, and its bottom bar is clipped behind the gesture bar.
  `safeDrawingPadding()` pads nothing.
- Cause: the dialog window keeps `fitInsetsTypes = STATUS_BARS | NAVIGATION_BARS`, so the window
  manager shrinks its frame (`dumpsys window windows` shows `fitTypes=` and a frame like
  `[0,63][1080,2337]`). Setting decor fits on the window is not enough.
- Fix: from the dialog's `DialogWindowProvider`, set the layout to `MATCH_PARENT`, call
  `WindowCompat.setDecorFitsSystemWindows(window, false)` and, on API 30+, set
  `window.attributes.fitInsetsTypes = 0`. See `ui/components/ExercisePickerScreen.kt`.
- Closed off by: the fix lives in one place; reuse `ExercisePickerScreen`'s helper for any new
  full-screen dialog and check the frame with `dumpsys` on an emulator.
- Seen: full-screen exercise picker, 2026-10-14.
