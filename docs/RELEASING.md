# Releasing & the in-app updater

How Workout Tracker (Ember) is signed, built, installed, and how the in-app updater ships new
versions. Companion doc for ring-set uses the same pattern.

## How the in-app updater works

Settings, **Updates**, **Check** runs [`UpdateService`](../app/src/main/java/com/lukr99/workout/update/UpdateService.kt)
through `UpdatesViewModel`. The chain follows CodePrint's auto-update seam:

1. **Release source:** `GitHubReleaseSource` reads
   `https://api.github.com/repos/lukr-99/workout-tracker/releases/latest`. The repository is
   public, so no token is needed. That is the chosen distribution model.
2. **Version policy:** a `-dev` build never checks. Otherwise the tag (without `v`) must be a
   higher dotted number than the installed `versionName`.
3. **Artifact selector:** the release must carry exactly `Ember-<version>.apk` and
   `Ember-<version>.apk.sha256`. A release without the checksum is shown as "cannot be verified"
   and is not offered.
4. **Verified download:** HTTPS only, before and after redirects. The APK streams to
   `cacheDir/updates/` as a `.part` file, and every announced byte must arrive. The SHA-256 must
   match the published checksum. Then the APK's package name and signing certificate must match the
   installed app's. Only then is the file kept.
5. **Installer launcher:** opens the system package installer through the FileProvider
   (`${applicationId}.files`, path `updates/`). Android checks the signature again and stays the
   final authority.

The manual path is always there: **All releases on GitHub** in the same section, or the USB phone
installer. Requires the `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions.

**The critical constraint — signatures must match.** Android only lets an APK upgrade an installed
app *in place* when both are signed with the **same key**. So:

- The very first release build must be **installed manually** (see below). It replaces whatever was
  there before, which requires an **uninstall** (data is wiped — export first).
- Every subsequent GitHub release APK must be signed with that **same release keystore**, or the
  updater's download will fail to install with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

## Signing: the release keystore

Release signing is wired in [`app/build.gradle.kts`](../app/build.gradle.kts) via a gitignored
`keystore.properties` at the repo root:

```
storeFile=keystore.jks
storePassword=<password>
keyAlias=workout
keyPassword=<password>
```

`keystore.jks` (also gitignored) is the actual signing key — RSA 2048, ~27-year validity, cert
SHA-256 `1db09253…`. **Without these two files a release build comes out UNSIGNED and cannot be
installed.**

### ⚠️ Back it up — losing it is unrecoverable

The keystore is **not in git** and cannot be regenerated to match. Lose it and you can never publish
another update that upgrades an installed copy in place — every user (i.e. you) would have to
uninstall (losing data) and reinstall from scratch. Keep **at least two** independent copies:

- Flash drive: `E:\android-keystores\workout-tracker\` (both files). Good for carrying between
  stations, not sufficient as the only copy.
- Plus one more: a cloud drive or a password manager's secure-file storage.

`keystore.properties` stores the password in **plaintext**; the `.jks` itself is password-protected.
For defense-in-depth, keep the password (`.properties`) somewhere separate from the key (`.jks`).

### Working across two stations

Copy both files into this repo's root on each machine. `local.properties` (`sdk.dir=`) is per-machine
and gitignored — set it to wherever that machine's Android SDK lives (this station:
`F:/DevTools/Android/Sdk`, which has build-tools 35 as pinned by `buildToolsVersion`).

## Build & install

**Dev / debug** (fast, debug-signed). A debug build is its own app: **Ember dev**,
`com.lukr99.workout.debug`, version `x.y.z-dev`. It installs beside the release app and never
touches its data. Instrumented test runs uninstall their app afterwards, which now only removes
Ember dev. The updater refuses to run in a `-dev` build.

```
./gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Release** (signed, distributable, R8/minify on):

```
./gradlew.bat :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk   (signed with keystore.jks)
```

First release install (or any signature change) needs a clean install:

```
# 1. In the app: Settings -> Data -> Export, save OUTSIDE the app (Downloads/Drive).
adb uninstall com.lukr99.workout
adb install app/build/outputs/apk/release/app-release.apk
# 2. In the app: Settings -> Data -> Import, pick the export.
```

Verify what's installed: `adb shell dumpsys package com.lukr99.workout | findstr version`.

## Publishing a release the updater will find

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, and rename `[Unreleased]` in
   `CHANGELOG.md` to `[<versionName>] - <date>`. If the release has something a user would
   notice, add a `WhatsNewNote` with the new `versionCode` to `domain/WhatsNew.kt`: three to five
   short lines. Home shows it once after the update. Merge that to `main`.
2. Tag the merge on `main` and push the tag:

   ```
   git switch main; git pull
   git tag v<versionName>
   git push origin v<versionName>
   ```

   [`.github/workflows/release.yml`](../.github/workflows/release.yml) then, on GitHub:
   - refuses to run without the four signing secrets, or when the tag is not `v<versionName>`;
   - builds `assembleRelease`, signed with the key from the secrets;
   - checks the APK is signed with the release key (certificate SHA-256 `1db09253...19e3`);
   - creates a **draft** GitHub Release `v<version>` with `Ember-<version>.apk`, its `.sha256`, and
     the changelog section as notes.

   **Actions, Release, Run workflow** by hand does the same but creates no release: it keeps the APK
   as a run artifact for a week, which makes a safe trial.
3. Review the draft on GitHub and publish it. Only a published release is visible to the updater.

**Without GitHub Actions**, from a station with the keystore in place: on a clean `main`, run
`.\tools\publish-release.ps1` (`-DryRun` stops before GitHub). It does the same checks and creates
the same draft. On a machine whose cmd AutoRun breaks batch files (`gradlew.bat`, `apksigner.bat`),
run Gradle as `java -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain`
and apksigner as `java -jar <sdk>\build-tools\35.0.0\lib\apksigner.jar`.

Installed copies from 2.5.2 and older pick the first `.apk` asset and ignore the checksum. They
update fine from a release made this way, and from then on they verify.

## Release signing on GitHub

The release workflow signs with four repository secrets: `KEYSTORE_BASE64` (the `.jks` file as
base64), `KEYSTORE_STORE_PASSWORD`, `KEYSTORE_KEY_ALIAS` and `KEYSTORE_KEY_PASSWORD`, as in
Tarot2Go. Upload them once from a station with the key:

```
powershell -ExecutionPolicy Bypass -File tools\setup-signing.ps1 -From D:\android-keystores\workout-tracker
```

The script reads `keystore.properties` and the keystore it names, refuses a keystore that does not
hold the release certificate, and uploads the secrets through `gh` without printing them or putting
them on a command line. It never makes a new key. Run it again only when the key or its password
changes. GitHub secrets are encrypted and only workflows can read them; the keystore and its
password are still never committed, because this repository is public. The offline backups above
stay the only way to recover the key.

## Current state (2026-11-12)

- Latest release: **v2.11.0** (`Ember-2.11.0.apk` and `Ember-2.11.0.apk.sha256`), published
  2026-11-12, made with `publish-release.ps1`'s steps from this station.
- Release signing on GitHub: the workflow is in place; the four secrets are uploaded with
  `tools/setup-signing.ps1`.
- Keystore backed up on the flash drive (`android-keystores\workout-tracker`); **still needs a second
  backup copy**.
