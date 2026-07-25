# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by Keep a Changelog, and this project currently uses simple semantic app versions for local releases.

## [1.0.0] - 2026-07-25

### Milestone

- Marks the `.NET MAUI` implementation as the **frozen 1.0 proof-of-concept**. This version
  is preserved as a historical stepping stone on the `release/1.0` branch and the `v1.0.0` tag.
- Consolidates the 0.1–0.3 line: reliable offline logging, templates, live sessions, exercise
  catalog with `wger` sync, list-first browse screens with floating navigation, full editing of
  completed workouts, toast notifications, persisted theme selection, and JSON/CSV export.

### Notes

- Active development continues on `feature/app-rework`, a ground-up rewrite as a **native
  Android (Kotlin + Jetpack Compose)** app. See `docs/rework/` for the migration plan.
- The MAUI app remains buildable from this tag as a reference for behaviour and data shape.

## [0.1.0] - 2026-04-01

### Added

- Android-first `.NET MAUI` app scaffold with local Git and GitHub-backed repository setup
- Dark-theme shell navigation with pages for Home, Templates, Catalog, History, Workout Detail, Workout Editor, and Settings
- Core workout domain model for exercises, templates, sessions, entries, strength sets, cardio data, analytics snapshots, and exports
- Local SQLite persistence layer with seeded exercises and history-safe workout snapshots
- Custom exercise support plus manual sync/import support for public exercise metadata through `wger`
- JSON and CSV export services for future desktop-manager compatibility
- Basic analytics-ready queries for workout history, consistency, and exercise progression
- Repository tests covering initialization, template-to-session behavior, analytics, and export inclusion
- Project documentation covering architecture decisions and roadmap milestones

### Notes

- The current version is a foundation release focused on reliable offline logging and future compatibility.
- Progression graphs and richer statistics are planned on top of the existing history and analytics service layer in future milestones.
