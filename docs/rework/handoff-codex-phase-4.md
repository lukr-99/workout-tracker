# Handoff Prompt — Phase 4 (Codex): Health Connect, backup automation & export v1.2

> **Status: ✅ COMPLETED & MERGED** into `feature/app-rework` — see
> [phase-4-services-report.md](phase-4-services-report.md). Kept for history. The UI grant/toggle
> flow for these services is release-hardening work (see README).

> A **non-UI** work package for Codex, parallel to Claude's Phase 4 UI polish. Pure `domain/` +
> `data/` service code with tests — **no `ui/` files, no screens.** Same model as your Phase 3 / 3.5.

---

You are extending the **Workout Tracker** data platform with three non-UI capabilities. The app now
carries Phases 0–3.5 on `feature/app-rework`; you built the Phase 3 services and the Phase 3.5
analytics/records/recovery/progression/wger layer. Work in an **isolated branch**
`codex/phase-4-integrations` off the current `feature/app-rework` tip, in a separate git worktree
(so it merges cleanly after Claude's Phase 4 UI). **Do not touch `ui/`.** Keep `AppContainer` edits
to additive `by lazy` entries; add stats providers additively as before.

## Read first

- `docs/rework/07-data-services.md` + `docs/rework/phase-3.5-report.md` — your existing platform
  (repository seams, `StatsEngine` providers, `ExportBundle`, `WorkoutInsightsService`,
  `mergeExternalExercisesDetailed`, `WgerSyncService`).
- `docs/rework/03-data-model.md` — schema, additive-field rules, `ExportBundle` versioning.
- `docs/rework/06-lyfta-study.md` — Lyfta syncs to **Health Connect**; that's the interop target.

## Scope (priority order)

### 1. Health Connect integration (`data/health/`, non-UI service)
A `HealthConnectService` that **reads and writes workout sessions** via Android **Health Connect**
(`androidx.health.connect:connect-client`):
- **Export:** map completed `WorkoutSession`s to Health Connect `ExerciseSessionRecord` (+ associated
  records where sensible) and write them; idempotent by a stable external key (reuse the SHA-256
  session fingerprint approach from Phase 3).
- **Import:** read `ExerciseSessionRecord`s back into `Exercise`/`WorkoutSession` drafts through the
  existing `WorkoutFactory` + repository seams (dedupe against existing sessions).
- Permissions are **requested by the UI later** — expose a `requiredPermissions` set and suspend
  `hasPermissions()/availability()` so Claude can wire the grant flow. Keep all Health Connect /
  Android types inside `data/health/`; nothing crosses into `domain/`. Add the `<uses-permission>`
  entries + the Health Connect manifest `<queries>`/privacy-policy activity stub (no UI).
- Return a structured summary (imported/exported/skipped/unsupported). Guard for devices without
  Health Connect installed.

### 2. Backup automation (`data/backup/`, WorkManager)
A scheduled **auto-backup** service: periodically produce the `ExportBundle` JSON (reuse
`dataTransfer`/`JsonExporter`) and write it to a user-chosen SAF tree (persisted URI) with a rolling
retention (keep last N). Implement as a `CoroutineWorker` + a `BackupScheduler` (enable/disable,
interval, last-run/last-result state exposed as a Flow). No UI — expose a clean API + a
`by lazy` `AppContainer.backup` entry Claude toggles from Settings. Unit-test the retention/naming
and the worker's do-work against a fake gateway.

### 3. Export format v1.2 + a couple of stats providers (small, additive)
- Bump `ExportBundle` to **`1.2`** to carry anything the above needs (e.g. a `source`/`externalKey`
  on sessions for Health Connect round-trips) — **additive only**; the reader must still accept
  `1.0/1.1/1.2` and ignore unknown fields. Update the importer tolerances + tests.
- Add 1–2 additive `StatsEngine` `MetricProvider`s that Phase 4 charts will want but don't exist yet
  (e.g. **estimated-1RM-over-time smoothing** and **weekly set-count per muscle** if not already
  covered by the recovery/body-part providers) — stable string keys, no request/response type
  changes. Only add what's genuinely missing; don't duplicate 3.5.

## Constraints

- **No UI.** No `ui/` files. Expose clean suspend/Flow APIs + document them; Claude renders them.
- `domain/` stays **Android-free**; Health Connect / WorkManager / Android types live only in
  `data/`. Reuse `WorkoutFactory`, `ExportBundle`, repository seams — don't duplicate Phase 1/3/3.5.
- Everything ships with tests (JVM where pure; instrumented for Health Connect mapping + the worker).
- Additive `AppContainer` entries only, so the merge after Claude's Phase 4 UI is a trivial union.
- If you change the schema, add a real Room `Migration` + a version case in `WorkoutMigrationTest`
  (never destructive fallback).

## Build / verify (this machine)

Canonical SDK `%LOCALAPPDATA%\Android\Sdk` (build-tools **35.0.0**, kept pinned); JDK 17 via
`~/.gradle/gradle.properties`. Stale-daemon "SDK location not found" → `.\gradlew.bat --stop`.
Verify `testDebugUnitTest` + `connectedDebugAndroidTest` on the Galaxy A56.

## Done when

Health Connect read/write, scheduled auto-backup, and export v1.2 exist as tested non-UI services
wired through additive `AppContainer` entries; all tests green. Write
`docs/rework/phase-4-services-report.md` listing the exact public APIs + permission requirements
Claude's UI must call, with one example each.
