# Workout Tracker

A native **Android** workout tracker built with **Kotlin + Jetpack Compose + Room** (MVVM,
offline-first, on-device). This is a personal-use app.

> **History:** v1 was a `.NET MAUI` / C# proof-of-concept. It's preserved on the **`release/1.0`**
> branch and tag **`v1.0.0`**. v2 is this ground-up native Kotlin rewrite (tag `v2.0.0`). The full
> migration story, architecture, and design system live in [`docs/rework/`](docs/rework/README.md).

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

Requires JDK 17 and the Android SDK (platform 35, build-tools 35). The Gradle wrapper fetches Gradle.

```powershell
.\tools\build-and-install.ps1 -Launch
```

Or with Gradle directly:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest   # on a connected device
```

A minified, self-signed **release** build is available if wanted — see
[`docs/rework/release-signing.md`](docs/rework/release-signing.md) — but nothing requires it.

## Documentation

- [`docs/rework/README.md`](docs/rework/README.md) — the rework index (architecture, design, phases)
- `docs/decisions.md`, `docs/roadmap.md` — **historical**, describe the v1 MAUI POC
