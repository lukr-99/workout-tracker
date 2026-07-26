# Handoff Prompt — Phase 5 (Codex): Release hardening + Phase-4 service UI

> Paste the block below as the opening prompt for Codex. It is self-contained.
>
> **Note on scope:** this package **includes UI work** — a deliberate departure from the earlier
> "Codex = non-UI" split, because the Phase-4 services shipped without their UI and this agent is
> taking it. Match the existing Compose UI exactly (see the design system + existing screens named
> below); this is wiring + store-readiness, not a redesign.

---

You are doing **Phase 5 — release hardening** of the native-Android **Workout Tracker**. The app is
feature-complete through Phase 4 on **`feature/app-rework`** in `F:\Code\workout-tracker` (Phases
0–4: shell, live-logging loop, Library/History/Progress/Settings, import/export, analytics/records/
recovery/progression, Health Connect + auto-backup **services**, Room schema v2). Work on branch
**`feature/app-rework`** directly, or cut `codex/phase-5-release` and hand it back for merge — your
call; if you branch, base it on the current tip and keep changes additive. The frozen `.NET MAUI`
app on `release/1.0` / tag `v1.0.0` is read-only reference.

## Read first

- `docs/rework/phase-4-report.md` **§6** and `docs/rework/phase-4-services-report.md` — the exact
  gaps you're closing and the **`AppContainer.healthConnect` / `AppContainer.backup` APIs**
  (signatures + examples + the 4 Health Connect permission strings).
- `docs/rework/02-design-system.md` — colors/type/components/motion; **match it**.
- Existing UI to mirror: `ui/screens/SettingsScreen.kt` + `ui/SettingsViewModel.kt` (where the new
  sections live), `ui/screens/LiveWorkoutScreen.kt` + `ui/LiveWorkoutViewModel.kt` (superset UI),
  `ui/components/*` (reuse `SectionCard`, `Chips`, `Dialogs`, `Toast`, `NumberPad`).
- Mirror **ring-set** conventions.

## Scope

### 1. Health Connect UI (Settings → "Health Connect" section)
Wire `AppContainer.healthConnect`:
- Show `availability()` state (Available / ProviderUpdateRequired → link to install/update Health
  Connect / Unavailable → hide or disable).
- **Permission grant flow** using the client's own contract
  (`PermissionController.createRequestPermissionResultContract()`), requesting
  `healthConnect.requiredPermissions`; reflect `hasPermissions()`.
- Actions: **Export to Health Connect** (`exportCompletedSessions()`) and **Import from Health
  Connect** (`importSessions()`), each with a running spinner and the `HealthConnectSyncSummary`
  (imported/exported/skipped/unsupported) in a toast/sheet. Keep it all off the main thread.
- **Replace the stub rationale screen**: `data/health/HealthConnectPermissionRationaleActivity` is a
  non-rendering stub — render a real **privacy/rationale** screen (Compose) explaining what health
  data is read/written and why. Add a matching in-app **Privacy Policy** screen (see item 4).

### 2. Automatic-backup UI (Settings → "Backup" section)
Wire `AppContainer.backup` (`state: Flow<BackupState>`, `enable(treeUri, intervalHours, retentionCount)`,
`disable()`):
- A toggle that, when enabled, launches `ACTION_OPEN_DOCUMENT_TREE` (SAF) to pick the backup folder,
  then calls `enable(...)`. Controls for interval (e.g. daily/weekly) and retention count.
- Show `BackupState`: enabled, chosen folder, interval/retention, **last run + result** (and failure
  message). A "Back up now" affordance is a nice-to-have.

### 3. Superset grouping UI (deferred from Phase 4)
In the live workout screen, let the user **group/ungroup consecutive entries** into supersets via the
existing `entries.supersetGroup` field (model + query already support it). Show a visual bracket/label
for grouped exercises. Keep it minimal-dark and touch-friendly.

### 4. Store-readiness (build + assets + CI)
- **App icon** (adaptive, ember on near-black, matching the theme) replacing the default launcher
  icon; **splash screen** via `androidx.core:core-splashscreen`.
- **Release build config**: enable **R8/minify + `shrinkResources`** for `release`, with ProGuard
  **keep rules** for Room, kotlinx.serialization (@Serializable), Health Connect, and OkHttp. Verify
  `assembleRelease` works (unsigned is fine for CI).
- **Signing — set up the config, do NOT create or embed secrets.** Add a `signingConfigs { release }`
  that reads `storeFile`/`storePassword`/`keyAlias`/`keyPassword` from a **git-ignored**
  `keystore.properties` (or env vars), and reference it from the `release` build type only. Add
  `keystore.properties` and `*.keystore`/`*.jks` to `.gitignore`. **Do not generate a keystore,
  invent passwords, or commit any credential** — document the `keytool` command the *user* runs to
  create their own keystore and the properties keys they fill in.
- **Versioning**: define a `versionCode`/`versionName` scheme (this is the first shippable —
  e.g. `versionName = "1.0.0"`, `versionCode = 1` for the *rework* line; note the POC was the old
  MAUI `v1.0.0`, so consider `2.0.0` to avoid confusion — flag the choice, don't guess silently).
- **Clear the compileSdk-35 warning**: either bump AGP to a version tested with 35 or add
  `android.suppressUnsupportedCompileSdk=35` to `gradle.properties`, whichever is cleaner.
- **CI**: a GitHub Actions workflow (`.github/workflows/`) that runs on push/PR — JDK 17, Android SDK,
  `./gradlew testDebugUnitTest assembleDebug lintDebug`. (Instrumented tests need a device/emulator;
  skip or make optional.)

## Constraints

- **Match the existing design system and components** — no visual redesign; reuse `ui/components/*`
  and the theme tokens. Don't regress the merged Phase 0–4 work.
- Consume the services via `AppContainer.healthConnect` / `backup` / etc. — **do not** reimplement
  Health Connect, backup, import/export, or stats logic in the UI.
- `domain/` stays Android-free; the repository stays the only Room/file boundary.
- **Never commit signing keys, keystores, or passwords.** Config reads them from git-ignored files
  the user provides.
- No Room schema change is expected; if one is truly needed, add a `Migration` (baseline `1.json` +
  `2.json` are checked in; `WorkoutMigrationTest` exists) — never destructive fallback.

## Build / verify (this machine)

Canonical SDK `%LOCALAPPDATA%\Android\Sdk` (build-tools **35.0.0**, kept pinned); JDK 17 via
`~/.gradle/gradle.properties`. Stale-daemon "SDK location not found" → `.\gradlew.bat --stop`.
Verify `testDebugUnitTest`, `assembleDebug`, **`assembleRelease`** (unsigned OK), and
`connectedDebugAndroidTest` on the Galaxy A56; `.\tools\build-and-install.ps1 -Launch` +
`.\tools\phone.ps1 screenshot` for the new Settings sections and superset UI.

## Done when

Health Connect connect/import/export and auto-backup are usable from Settings with a real rationale +
privacy screen; superset grouping works in the live screen; the app has a real icon + splash; a
signed-*capable* minified release build assembles (no secrets committed); CI is green; the compileSdk
warning is gone. Then write `docs/rework/phase-5-report.md` (same style) and list what remains for an
actual Play Store submission (screenshots, store listing copy, content rating, the hosted privacy URL).
