# Ember

**Ember** is a native **Android** fitness app built with **Kotlin + Jetpack Compose + Room** (MVVM,
offline-first, on-device): strength training plus a Strava-style **Run Mode** (live GPS runs with a
dark map, pace/splits, routes). This is a personal-use app.

> **History:** v1 was a `.NET MAUI` / C# proof-of-concept. It's preserved on the **`release/1.0`**
> branch and tag **`v1.0.0`**. v2 is this ground-up native Kotlin rewrite (tag `v2.0.0`). The full
> migration story, architecture, and design system live in [`docs/rework/`](docs/rework/README.md).

## Status

In daily personal use on one phone. The latest release is
[v2.5.2](https://github.com/lukr-99/workout-tracker/releases/tag/v2.5.2) (2026-09-20). Unreleased
work is listed under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md). Strength logging, Run Mode,
backup and the updater are stable. The Spotify control only opens Spotify for now.

## Requirements

- Android 8.0 (API 26) or newer. Release builds are arm64 only.
- To build: JDK 17 or newer, Android SDK platform 35 and build-tools 35.
- Optional: Health Connect on the phone, and internet access for updates, catalog sync, map tiles
  and route snapping.

## Features

- 5-item shell with a central **Start** action; fast live logging (touch numpad, REPS/KG, set types,
  RIR/RPE, timed sets, supersets, rest timer)
- Reusable templates; searchable/filterable exercise catalog with custom exercises and **photos**
  (your own, plus wger / free-exercise-db imagery)
- Full history with after-the-fact editing
- **Progress**: per-exercise e1RM/volume charts, **records/PRs**, and a **muscle-recovery** body map
- Import/export (portable JSON + CSV; **Lyfta CSV importer**), **Health Connect** sync, and scheduled
  automatic backup
- Minimal-dark design with an ember accent

## Project layout

- `app/` — the Android app
  - `data/` — Room entities/DAO/db, repository (the only IO boundary), import/export, services
    (Health Connect, backup, wger sync, images)
  - `domain/` — pure Kotlin (no Android imports): analytics, records, recovery, progression, estimates
  - `ui/` — Compose: `App.kt` shell, per-area ViewModels, one-file-per-screen, reusable `components/`
- `docs/rework/` — architecture, data model, design system, feature roadmap, phase reports
- `tools/` — PowerShell helpers for building/installing and pulling data off a USB-connected phone

## Build, install, run (personal)

For a non-programmer on Windows, create a self-contained phone installer:

```powershell
.\tools\make-phone-installer.ps1
```

Send the resulting `dist/Ember-Phone-Installer-v*.zip`. The recipient extracts it, connects an
unlocked phone with USB debugging enabled, and double-clicks **Install Ember.cmd**. The guided
installer downloads Google's Android platform tools, installs or upgrades Ember without clearing
compatible app data, and opens it. Android requires USB debugging for computer-driven installs;
the included `README.txt` walks through that one-time phone setting.

To build from source and install a debug build on a connected phone:

```powershell
.\tools\build-and-install.ps1 -Launch
```

A debug build is its own app, **Ember dev**, so it installs beside the real Ember and never touches
its data. Test commands are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for modules, dependency direction, data flow and known
constraints, and [CONTEXT.md](CONTEXT.md) for the words the app uses.

## Data safety

- All data stays on the phone in a Room database (`workout.db`), plus settings in DataStore and
  personal exercise photos in the app's files.
- Schema changes use additive Room migrations with no destructive fallback. Every schema from 1 to
  8 is checked in under `app/schemas/` and covered by migration tests.
- **Manual backup:** Settings, Data, Save JSON. The file (format 1.8) holds everything: workouts,
  runs, routes, templates, exercises, photos and settings.
- **Automatic backup:** daily or weekly into a folder you choose, keeping the newest N files.
- **Restore:** import the file. You see a preview first, then choose Merge (add to what is there)
  or Replace everything (put the backup back exactly). A failed restore changes nothing.
- **Delete all data:** in the danger zone at the end of the Data screen.
- What is stored where, and the full rules, are in [docs/data-contract.md](docs/data-contract.md).
  Recovery steps are in [SECURITY.md](SECURITY.md#recovery).

## Delivery

- **Release:** `com.lukr99.workout`, signed with the owner's keystore and published as a GitHub
  Release with the APK. The in-app updater (Settings, Updates) installs newer releases in place.
- **Debug:** `com.lukr99.workout.debug`, labelled Ember dev, version `x.y.z-dev`. It never updates
  itself.
- **Manual path:** the USB phone installer above, or `adb install -r` with a release APK.
- Signing, keystore backup and publishing steps are in [docs/RELEASING.md](docs/RELEASING.md).

## Documentation

- [docs/pitfalls.md](docs/pitfalls.md): mistakes this repository already made. Search it first.
- [docs/rework/README.md](docs/rework/README.md) and [docs/run-mode/README.md](docs/run-mode/README.md):
  the history of the v2 rewrite and Run Mode (plans, phase reports, design notes).
- `docs/decisions.md`, `docs/roadmap.md`: **historical**, describe the v1 MAUI POC.


## License

[PolyForm Noncommercial 1.0.0](LICENSE.md) — free for personal and non-commercial use; selling or other commercial use requires permission.
