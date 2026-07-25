# Handoff Prompt — Phase 3.5 (Codex): Analytics, records, progression & catalog sync services

> A **non-UI** work package for Codex, parallel to Claude's Phase 2 UI work. Everything here is pure
> `domain/` + `data/` service code with unit tests — **no `ui/` files, no screens.** It extends the
> Phase 3 data platform you built and produces stable APIs that Phase 2 screens render.

---

You are extending the **Workout Tracker** native-Android data platform with analytics, records,
progression, and catalog-sync **services**. This is a follow-on to your Phase 3 work.

**Branch/worktree:** work in an **isolated branch** `codex/phase-3.5-analytics` off the current
`feature/app-rework` tip (which now contains your merged Phase 3 + the Phase 1 data core), in a
separate git worktree — same model as Phase 3, so it merges cleanly later. **Do not touch `ui/`**
(Claude owns Phase 2 UI in parallel). Keep `AppContainer` edits minimal and additive (new `by lazy`
entries only), and add any new `domain/` providers the same additive way you designed in Phase 3.

## Read first

- `docs/rework/07-data-services.md` — your Phase 3 platform (StatsEngine, `MetricProvider`/
  `DimensionProvider`, `WorkoutQuery`, services). You are adding providers/services in this style.
- `docs/rework/06-lyfta-study.md` — the Lyfta features these APIs feed (Muscle Recovery card,
  per-exercise Records, progression). Screenshots in `docs/rework/reference/lyfta/`.
- `docs/rework/03-data-model.md` — the schema (body parts, set types, e1RM fields).
- `WorkoutTracker.Core/Services/WgerSyncService.cs` — the MAUI catalog-sync to port (item 4).

## Scope (priority order)

### 1. Records / PR engine (pure `domain/`)
A `RecordsEngine` computing **personal records per exercise** from history: heaviest set, best
estimated 1RM (reuse `Estimates`), best set volume, best session volume, and **per-rep-max** (best
weight at each rep count → a 1–12 rep-max table). Warmups excluded. Returns immutable models with the
source session id/date for each record. This backs the Progress detail **"Records"** surface and the
live-logging **"new PR"** flag. Deterministic + fully unit-tested.

### 2. Muscle-recovery / body-part volume model (additive StatsEngine providers)
New `DimensionProvider`/`MetricProvider`s (plus a small `RecoveryEngine`) that produce:
- **weekly volume + set count per body part** (primary + secondary from the exercise snapshot), and
- a **per-muscle "readiness/recovery" score** (0–100) from recency & load of recent training — the
  model behind Lyfta's *Muscle Recovery* body map. Tunable decay; pure function of history + clock.
Expose via the existing `StatsRequest` keys so it's additive (no request/response type changes).

### 3. Progression suggestion engine (pure `domain/progression`)
Given an exercise's recent history + a scheme, suggest the **next session's target sets** (reps×kg):
double-progression (add reps to top of range, then add weight and reset), simple linear (+X kg when
all sets hit), and %-of-e1RM. Include a basic **deload** trigger (stall over N sessions). Pure
functions returning suggestions + rationale; no persistence. Feeds the live-workout "suggested next
set" prefill.

### 4. wger catalog sync service (`data/`, non-UI) — port of the MAUI service
Port `WgerSyncService.cs`: fetch public exercise metadata from the **wger** API, map to `Exercise`
(with `ExternalSourceId`, `Source = Synced`), and merge **additively** through the existing
`repository.mergeExternalExercises(...)` seam (never overwrite custom/edited records). Add the HTTP
client dependency (prefer Ktor-CIO or OkHttp; keep it isolated in `data/`), the `INTERNET`
permission, tolerant parsing, paging, and cancellation. Return a structured sync summary
(added/updated/skipped). This is the one Android/network-coupled item — keep all Android/network
types inside `data/`, not `domain/`.

### 5. Migration-test scaffold (small, closes phase-1 debt)
Add a Room `MigrationTestHelper` instrumented test using the checked-in `app/schemas/1.json`
baseline, so the first real schema change in a later phase has migration coverage ready. No schema
change now.

## Constraints

- **No UI.** No files under `ui/`. If a screen needs to render this, that's Claude's Phase 2 — just
  expose a clean API + document it.
- `domain/` stays **Android-free** (items 1–3, 5-domain-parts). Android/network types live only in
  `data/` (item 4).
- Extend, don't rewrite: reuse `StatsEngine` provider extension points, `Estimates`, `WorkoutQuery`,
  and the repository seams. Don't duplicate Phase 1/3 logic.
- Everything ships with **JVM unit tests** (items 1–3, 5) / instrumented tests (item 4 sync mapping,
  item 5 migration). Match your Phase 3 test rigor.
- Keep `AppContainer` changes to additive `by lazy` service entries so the later merge with Phase 2
  is trivial.

## Build / verify (this machine)

Canonical SDK `%LOCALAPPDATA%\Android\Sdk` (build-tools **35.0.0**, kept pinned); JDK 17 via
`~/.gradle/gradle.properties`. If Gradle reports "SDK location not found" despite a valid
`local.properties`, it's a stale daemon — `.\gradlew.bat --stop` then rebuild. Verify with
`testDebugUnitTest` + `connectedDebugAndroidTest` on the Galaxy A56.

## Done when

Records, recovery/body-part, and progression engines exist as tested pure-Kotlin APIs; wger sync
lands as a `data/` service wired through `mergeExternalExercises`; the migration-test scaffold is in
place; all tests green. Write `docs/rework/phase-3.5-report.md` listing the exact public APIs Phase 2
should call (signatures + one example each), so Claude can wire the Progress/Home/live-logging
screens to them.
