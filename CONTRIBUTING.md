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
