# AGENTS

Ember is a personal Android workout app: strength logging plus a GPS Run Mode. Kotlin, Jetpack
Compose and Room, offline first, one `:app` module. It follows the CodePrint baseline, which usually
sits beside this repository (`../CodePrint`).

## Required context

1. Read `ARCHITECTURE.md` before changing module seams or dependency direction.
2. Read `CONTEXT.md` before introducing domain terms, and use its words in code and copy.
3. Read `docs/RELEASING.md` before touching signing, versions or the updater.
4. `docs/rework/` and `docs/run-mode/` hold the history of how the app was built. Treat them as
   background, not as the current truth.

## Baseline

- One top-level type per matching file. `.codeprint.json` lists the files that still break this
  rule. That list is debt: it may only shrink, and a file you split comes off it.
- Constructor injection, with `data/AppContainer.kt` as the composition root. Do not add new
  `application as WorkoutApp` lookups.
- `domain/` has no Android imports. The repository is the only class that touches Room.
- Add or update deterministic tests with every behavior change.
- A Room schema change needs a new numbered migration file in `data/migrations/`
  (`MigrationNNNNWhatChanged`, added to `WorkoutMigrations.ALL`), the generated schema JSON checked
  in, an isolated `N-1 -> N` test and the full chain in `WorkoutMigrationTest`. Never use a
  destructive fallback.
- A change to the export bundle bumps `ExportBundle.CURRENT_VERSION`, keeps every older version in
  `SUPPORTED_VERSIONS`, and extends the round-trip tests.
- Use Conventional Commits and several coherent commits for independent slices. Add user-visible
  changes to the `[Unreleased]` section of `CHANGELOG.md` in the same commit.
- Plain English in all text and copy. No em-dashes.

## Devices

- Debug builds are their own app: **Ember dev**, `com.lukr99.workout.debug`, version `x.y.z-dev`.
  The owner's phone runs the release-signed `com.lukr99.workout`. Never install, clear or uninstall
  the release app.
- Instrumented tests uninstall their app when they finish and install on every attached device.
  Pin them to an emulator: `$env:ANDROID_SERIAL = 'emulator-5554'`.
- In scripts, name components with the full class name (`$DebugPackage/$ClassPrefix.MainActivity`
  from `tools/common.ps1`). The `pkg/.Class` shorthand expands against the suffixed package.

## Pitfalls

- `docs/pitfalls.md` lists mistakes this repository already made. When something fails in a way you
  did not expect, search it and CodePrint's `docs/pitfalls/` for the error text before debugging.
- When a bug took longer to find than to fix, came back, or came from a tool trap, add an entry in
  the same commit as the fix. Add a test or check that catches it when you can.
- If it could hit another repository, report it as a `pitfall` item on the CodePrint project in
  GoalMaker (CodePrint's `docs/pitfalls/README.md`). Do not edit CodePrint from this repository.
- Windows PowerShell 5.1 misreads UTF-8 files. Edit sources with an editor or a tool that keeps the
  encoding, not with `Get-Content` and a rewrite.

## Tracking

- This repository's board is its GoalMaker project (Ember): `find_project` with this folder.
- Move an item to Doing when you start it. Put bugs and ideas you find but do not fix on the board.
- A pull request lists its items as `GoalMaker: <item id>` lines, and items reach Done only when the
  work is merged to `main` (CodePrint's `docs/project-tracking.md`).

## Verification

Pick the cheapest check that answers the question (CodePrint `docs/android-agent-workflow.md`):

```powershell
py -B tools/validate_repository.py --root .     # python on Linux and in CI
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat recordRoborazziDebug              # then open app\src\test\screenshots\*.png
.\tools\emulator.ps1 start                      # headless emulator, prints its serial
.\tools\ui-check.ps1 -PackageName com.lukr99.workout.debug   # build, install, launch, screen.png + layout.xml
$env:ANDROID_SERIAL = '<emulator serial>'; .\gradlew.bat connectedDebugAndroidTest
.\tools\agent-doctor.ps1                        # what is installed, with fixes
```

- Test tags live in `AppTags`, `LiveWorkoutTags` and `DataTags`. The root sets `testTagsAsResourceId`,
  so they appear as `resource-id` in `layout.xml` and as Maestro `id:` selectors. A bottom sheet is its
  own window and must set `testTagsAsResourceId` again.
- Prefer `layout.xml` over a screenshot to check text or state.
- Robolectric tests that render Compose use `@Config(application = android.app.Application::class)`,
  because `WorkoutApp` starts Room work that leaks into the next test.

## CI with Kiln

On the owner's PC, run CI in Kiln first. It is a local runner for the GitHub Actions workflows, and
it runs jobs in Linux Docker containers. The `.kiln/workflows/` files mirror `.github/workflows/`.
Start the daemon (`kiln daemon` or the Kiln tray app) if `http://127.0.0.1:7878/` does not answer.

```powershell
kiln run .kiln/workflows/codeprint.yml --repo .   # repository validation, PowerShell syntax
kiln run .kiln/workflows/android.yml --repo .     # unit tests, lint, debug APK
kiln run .kiln/workflows/web.yml --repo .         # web build, node tests, stamp, Chromium check
kiln run .kiln/workflows/cloud.yml --repo .       # Claude server typecheck and tests
```

- Kiln copies the working tree, so uncommitted changes are tested. Gradle and Maven caches (the
  Robolectric `android-all` jars too) stay in Kiln cache volumes, so only the first run downloads.
- Kiln only: nothing. Every Kiln job has a GitHub twin.
- GitHub only: instrumented tests (they need an emulator with KVM), the Cloudflare deploy (secrets),
  and the signed release (`release.yml`, needs the signing secrets).
  The first two have a Kiln job that only prints why it is skipped. The release has no Kiln file yet.
- GitHub Actions stays the fallback and runs on every push and pull request. Do not delete or
  disable it. Keep a Kiln file in step when you change its GitHub twin.
