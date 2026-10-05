# Contributing

## Local verification

You need JDK 17 or newer and the Android SDK (platform 35, build-tools 35). Point `ANDROID_HOME`
or `local.properties` at the SDK. The Gradle wrapper fetches Gradle.

```powershell
# Repository baseline (use `python` instead of `py` outside Windows)
py -B tools/validate_repository.py --root .

# Build, unit tests and lint, as CI runs them
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug

# Room, migration and instrumented UI tests, pinned to an emulator
$env:ANDROID_SERIAL = 'emulator-5554'
.\gradlew.bat connectedDebugAndroidTest
```

CI also runs the instrumented tests on a Gradle managed device (`pixel2Api35DebugAndroidTest`) and
parses every PowerShell script under `tools/` and `installer/`.

### Screenshots and the emulator loop

- `ComponentScreenshotTest` renders key components in light and dark on the JVM (Robolectric and
  Roborazzi). `testDebugUnitTest` runs them as a smoke test. `recordRoborazziDebug` rewrites the
  PNGs in `app/src/test/screenshots/`, and `compareRoborazziDebug` writes diff images. Record and
  compare on the same OS; Windows and Linux render slightly differently.
- `tools/emulator.ps1 start` boots a headless emulator. `tools/ui-check.ps1 -PackageName
  com.lukr99.workout.debug` builds, installs without clearing data, launches, and saves a
  screenshot, the UI hierarchy and logcat under `artifacts/ui-check/`. It picks the emulator when a
  phone is also attached.
- `.maestro/launch-smoke.yaml` is the smallest end-to-end journey. Run it with
  `tools/ui-check.ps1 ... -Flow .maestro` once Maestro is installed (`tools/agent-doctor.ps1` says
  how).

To try a build on a phone, `.\tools\build-and-install.ps1 -Launch` installs **Ember dev** beside
the release app. Release builds and publishing are described in `docs/RELEASING.md`.

## Change shape

- Keep each commit to one coherent behavior or repository change.
- Use Conventional Commits: `type(optional-scope): imperative summary`.
- Keep tests and documentation in the same commit as the behavior they protect.
- Add user-visible changes to `[Unreleased]` in `CHANGELOG.md`.
- A Room schema change brings its migration, the generated schema JSON and its migration tests.
- Do not commit builds, secrets, signing files, `local.properties`, device serials or personal data
  pulled off a phone (`import/` is ignored for that).

## Pull requests

- Work on a short-lived branch and open a pull request into `main`.
- State the outcome, verification, screenshots for UI work, data and migration impact, and
  rollback or recovery for risky delivery changes.
- List the GoalMaker items the pull request closes, one per line: `GoalMaker: <item id>`.
- CI must pass before merge.
