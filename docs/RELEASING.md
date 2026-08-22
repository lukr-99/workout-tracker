# Releasing & the in-app updater

How Workout Tracker (Ember) is signed, built, installed, and how the in-app updater ships new
versions. Companion doc for ring-set uses the same pattern.

## How the in-app updater works

Settings → **Updates** → **Check** calls
[`AppUpdater`](../app/src/main/java/com/lukr99/workout/update/AppUpdater.kt), which:

1. GETs `https://api.github.com/repos/lukr-99/workout-tracker/releases/latest` (public API, no auth).
2. Parses the release `tag_name` and the first asset whose name ends in `.apk`.
3. Compares the tag (minus a leading `v`) against the installed `versionName` (read from
   `PackageManager`) with a dotted-numeric comparison.
4. If newer: downloads the APK into `cacheDir/updates/`, then launches the system package installer
   via a `FileProvider` content URI (`${applicationId}.files`, path `updates/`).

No third-party dependencies — `HttpURLConnection` + `org.json` + coroutines. Requires the
`INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions (both declared in the manifest).

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

- Flash drive: `G:\android-keystores\workout-tracker\` (both files) — good for carrying between
  stations, not sufficient as the only copy.
- Plus one more: a cloud drive or a password manager's secure-file storage.

`keystore.properties` stores the password in **plaintext**; the `.jks` itself is password-protected.
For defense-in-depth, keep the password (`.properties`) somewhere separate from the key (`.jks`).

### Working across two stations

Copy both files into this repo's root on each machine. `local.properties` (`sdk.dir=`) is per-machine
and gitignored — set it to wherever that machine's Android SDK lives (this station:
`F:/DevTools/Android/Sdk`, which has build-tools 35 as pinned by `buildToolsVersion`).

## Build & install

**Dev / debug** (fast, debug-signed — cannot receive release-signed updates):

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

1. Bump `versionCode` + `versionName` in `app/build.gradle.kts` and add a `CHANGELOG.md` entry.
2. `./gradlew.bat :app:assembleRelease`.
3. Create a **GitHub Release** (not just a tag) tagged `v<versionName>` and **attach
   `app-release.apk`**.
4. Ensure the repo's Releases are **public** (the updater hits the unauthenticated API).

The next time any installed copy taps **Check**, it finds the release and updates in place.

## Current state (2026-08-22)

- Installed on the phone: **v2.3.0**, release-signed with `keystore.jks`. Updater UI verified live.
- Keystore created and backed up to the flash drive; **still needs a second backup copy**.
- No GitHub Release published yet — until one exists with an APK asset, Check reports "You're on the
  latest version."
