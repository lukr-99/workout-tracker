# Workout Tracker — Native Android Rework

This folder is the plan for rebuilding Workout Tracker as a **native Android app**
(Kotlin + Jetpack Compose + Room + MVVM), replacing the `.NET MAUI` proof-of-concept
that shipped as [`v1.0.0`](../../CHANGELOG.md) and is preserved on the `release/1.0` branch.

The rework is happening on `feature/app-rework`. Nothing here is built yet — these are the
design and planning documents we execute against.

## Why rework

The MAUI 1.0 POC proved the domain model and the flows (templates → live sessions →
history → analytics-ready data → export). It did its job. But:

- The primary and only real target is **Android**, and MAUI adds a cross-platform runtime
  tax we don't use.
- We want **Lyfta-grade visuals** — fluid Compose animation, custom charts, a cohesive
  minimal dark system — which is far more natural in Compose than in MAUI/XAML.
- We want to mirror the clean, layered structure of the **ring-set** app (our other native
  Android project): pure `domain/`, Room-backed `data/`, one-file-per-screen Compose `ui/`.

## Design north star

- **Native Android first.** Compose UI, Room storage, everything on-device, offline-first.
- **Minimal dark, Lyfta-inspired, our own spin.** See [02-design-system.md](02-design-system.md).
- **Portable core.** `domain/` and `data/` avoid Android-only types where practical so a
  *separate* future Windows data tool can consume the same exported data. We do **not** adopt
  Kotlin Multiplatform now — the seam is the data/export format, not shared code.
- **Data is the contract between devices.** A stable, versioned JSON export/import format is a
  first-class feature, not an afterthought — it's how the phone and any future desktop tool talk.

## Documents

| Doc | What it covers |
|-----|----------------|
| [00-migration-plan.md](00-migration-plan.md) | Phased plan to get from 1.0 POC to a shipping native app |
| [01-architecture.md](01-architecture.md) | Target module/package structure, layers, tech choices |
| [02-design-system.md](02-design-system.md) | Lyfta-inspired minimal dark design: color, type, components, motion |
| [03-data-model.md](03-data-model.md) | Room schema, mapping from the MAUI domain, migrations |
| [04-feature-roadmap.md](04-feature-roadmap.md) | Full future feature set + milestones |
| [05-lyfta-import.md](05-lyfta-import.md) | Pulling existing data out of Lyfta and importing it |
| [../../tools/README.md](../../tools/README.md) | Phone/ADB omni-tooling for building, installing, and pulling data |

## Status snapshot

- [x] Freeze MAUI app as `v1.0.0` on `release/1.0`
- [x] Open `feature/app-rework`
- [x] Write this plan
- [x] Phone/ADB tooling scaffold
- [ ] Lyfta data extraction (needs USB debugging authorized)
- [x] Scaffold the Gradle/Compose project — Phase 0 (authored; on-device build pending an
  Android SDK + JDK 17 on the build machine, see `tools/env-local.md`)
