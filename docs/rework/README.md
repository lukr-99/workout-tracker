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
| [06-lyfta-study.md](06-lyfta-study.md) | Lyfta capability backlog to study and fold into the roadmap |
| [07-data-services.md](07-data-services.md) | Phase 3 creation/query/stats/import/export service contracts |
| [phase-0-report.md](phase-0-report.md) | Phase 0 scaffold review + open decisions |
| [phase-1-report.md](phase-1-report.md) | Phase 1 data-core implementation + verification report |
| [phase-3-report.md](phase-3-report.md) | Phase 3 implementation and verification report |
| [phase-3.5-report.md](phase-3.5-report.md) | Phase 3.5 records/recovery/progression/catalog-sync report and UI APIs |
| [handoff-phase-1.md](handoff-phase-1.md) | Prompt for the Phase 1 data-core agent (done) |
| [handoff-phase-2.md](handoff-phase-2.md) | Prompt for the Phase 2 logging-loop UI agent (Claude) |
| [handoff-codex-phase-3.5.md](handoff-codex-phase-3.5.md) | Parallel non-UI package for Codex: analytics/records/progression/wger sync (done) |
| [phase-2-report.md](phase-2-report.md) | Phase 2 UI implementation + verification report |
| [handoff-phase-4.md](handoff-phase-4.md) | Prompt for Phase 4 UI/polish (Claude): set-input rework, wire insights/wgerSync, motion |
| [handoff-codex-phase-4.md](handoff-codex-phase-4.md) | Parallel non-UI package for Codex: Health Connect, backup automation, export v1.2 |
| [../../tools/README.md](../../tools/README.md) | Phone/ADB omni-tooling for building, installing, and pulling data |

## Status snapshot

- [x] Freeze MAUI app as `v1.0.0` on `release/1.0`
- [x] Open `feature/app-rework`
- [x] Write this plan
- [x] Phone/ADB tooling scaffold
- [x] Lyfta data extraction — 468 sets / 19 sessions captured (`import/lyfta/`)
- [x] **Phase 0** — scaffold the Gradle/Compose project (verified on-device; see phase-0-report)
- [x] **Phase 1** — data core (Room + repository + domain). Verified on-device; see [phase-1-report.md](phase-1-report.md)
- [x] **Phase 3** — data services (import/export + creation/query/stats), built by Codex on
  `codex/phase-3-data-services` and **merged into `feature/app-rework`**; see
  [phase-3-report.md](phase-3-report.md) and [07-data-services.md](07-data-services.md)
- [x] **Phase 2** — core logging loop UI: 5-item shell + central Start, live logging loop, Home /
  Library / History / Progress / Settings over the Phase 1/3 seams. Verified on-device; see
  [phase-2-report.md](phase-2-report.md).
- [x] **Phase 3.5** — non-UI records, recovery/body-part stats, progression, and wger catalog sync
  (Codex), **merged into `feature/app-rework`**; see [phase-3.5-report.md](phase-3.5-report.md).
- [ ] **Phase 4** — polish + advanced features. UI/integration (Claude):
  [handoff-phase-4.md](handoff-phase-4.md) — set-input rework, add-exercise to past workouts, wire
  `insights`/`wgerSync` (Records, Muscle Recovery, progression), motion, cardio. Parallel non-UI
  (Codex): [handoff-codex-phase-4.md](handoff-codex-phase-4.md) — Health Connect, backup automation,
  export v1.2.
- [ ] Deeper Lyfta capability study → roadmap ([06-lyfta-study.md](06-lyfta-study.md))
