# Phase 5 Report — Release hardening + service UI

> **Branch:** `codex/phase-5-release`  
> **Worktree:** `F:\Code\workout-tracker-phase-5-release`  
> **Base:** `feature/app-rework` at `d127d5b`

## 1. TL;DR

The Phase 4 Health Connect and automatic-backup services now have usable Settings UI, including
the platform permission and SAF folder flows. Health Connect's manifest rationale endpoint now
renders the same real privacy policy available in-app. Consecutive live-workout exercises can be
joined into and removed from supersets with a visible ember bracket. The native app is versioned
as 2.0.0, has a splash/themed adaptive icon, produces a minified resource-shrunk release APK,
supports secret-free local/CI signing configuration, and has an Android GitHub Actions workflow.

## 2. Health Connect

- Settings reports `Available`, `Provider update required`, or `Unavailable` from
  `AppContainer.healthConnect.availability()`.
- The Connect action uses Health Connect's own
  `PermissionController.createRequestPermissionResultContract()` and requests the service's four
  `requiredPermissions`.
- Import and Export actions call the existing service off the main thread, show a running spinner,
  and summarize imported/exported/skipped/unsupported counts in the app toast.
- Provider-update state opens the Health Connect Play listing.
- `HealthConnectPermissionRationaleActivity` is now a real Compose screen rather than a finishing
  stub. The same policy is reachable from Settings and describes exercise/weight reads and writes,
  purpose, storage, optional backup, and revocation.

On the Galaxy A56, Health Connect reported Available and the Connect action opened the system
Health Connect permission controller. Permissions were deliberately not granted during automated
verification because health-data consent belongs to the user.

## 3. Automatic backup

- Settings observes `AppContainer.backup.state` and renders enabled state, selected tree, interval,
  retention, last run/result, and any failure message.
- Enabling opens `ACTION_OPEN_DOCUMENT_TREE` through `OpenDocumentTree`; the returned URI is passed
  to `BackupScheduler.enable`.
- Daily/weekly intervals and retention choices of 3/7/14 are supported. Changing a value while
  enabled reschedules against the same persisted tree URI.
- Disabling calls `BackupScheduler.disable`.

The system document-tree picker was verified on device. No folder was selected during the
walkthrough, leaving the user's storage-location choice untouched.

## 4. Superset grouping

- Every exercise after the first has a link action that toggles its boundary with the preceding
  exercise.
- Joining adjacent grouped runs merges them. Ungrouping can split a longer run, and singleton
  fragments are cleared.
- Removal and reordering normalize group membership so a superset never silently spans a
  non-consecutive exercise.
- Grouped cards render an ember side bracket, `SUPERSET n` label, and an orange unlink state.
- Four JVM tests cover pair join/unjoin, adjacent-run merge, middle split, and non-consecutive
  normalization.

## 5. Store and build hardening

- Preserved the existing ember dumbbell adaptive icon and added a single-color themed-icon mask.
- Added `androidx.core:core-splashscreen` with the near-black background and dumbbell mark.
- Native release version is **2.0.0** (`versionCode = 1`) to avoid confusing it with the frozen
  MAUI `v1.0.0` proof-of-concept.
- Release enables R8 and resource shrinking. Rules cover Room, the kotlinx.serialization contracts,
  Health Connect, OkHttp, and optional TLS providers.
- Release signing reads a git-ignored `keystore.properties` or four `KEYSTORE_*` environment
  variables. With neither present, CI still assembles an unsigned release APK.
- `.gitignore` excludes `keystore.properties`, `*.keystore`, and `*.jks`.
- `docs/rework/release-signing.md` documents the user-run `keytool` command and required keys.
- The pinned AGP 8.5.2 / compileSdk 35 pairing is explicitly acknowledged in `gradle.properties`,
  clearing the unsupported-compileSdk warning without destabilizing Kotlin/KSP.
- `.github/workflows/android.yml` provisions JDK 17 and Android 35, then runs JVM tests, debug
  assembly, and debug lint on push and pull request.

## 6. Verification

Galaxy A56 (`SM-A566B`, Android 16, `RZCY60P9EHB`):

- `testDebugUnitTest` — passed.
- `assembleDebug` — passed.
- `lintDebug` — passed (non-blocking dependency/toolchain and pre-existing locale warnings only).
- `assembleRelease` — passed with R8 + resource shrinking; unsigned APK generated.
- `connectedDebugAndroidTest` — all 17 tests passed.
- Debug APK installed and cold-launched.
- Health Connect permission controller, SAF tree picker, in-app policy, rationale activity, and
  live superset group/ungroup were exercised manually.

Device captures:

- `phase-5-settings.png`
- `phase-5-backup.png`
- `phase-5-privacy.png`
- `phase-5-superset.png`

## 7. Remaining for Play submission

- User creates and securely stores the upload keystore, then performs a signed release build.
- Host the privacy policy at a public HTTPS URL and enter that URL in Play Console.
- Capture final phone/tablet store screenshots and prepare feature graphic, icon export, and
  listing copy.
- Complete Play content rating, Data safety, Health apps declaration, target-audience, and app
  access forms.
- User grants Health Connect permissions and performs an end-to-end import/export with their own
  health data.
- User selects the intended backup folder and confirms a real scheduled backup after WorkManager's
  inexact execution window.
