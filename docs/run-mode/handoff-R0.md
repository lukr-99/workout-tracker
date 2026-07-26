# Handoff Prompt — Run Mode R0: Foundation (rebrand, shell, map, run data model)

> Paste the block below as the opening prompt for the agent. Self-contained. Touches `ui/` + `data/`
> + `domain/`. First phase of Run Mode (v2.1) on branch `feature/run-mode`.

---

You are building **R0 — the foundation** for **Run Mode (v2.1)** of the app (native Android,
Kotlin/Compose/Room/MVVM, minimal-dark + ember; the strength app shipped as v2.0.0). Work on branch
**`feature/run-mode`** in `F:\Code\workout-tracker` (already checked out; commit + push there). Read
`docs/run-mode/README.md`, `architecture.md`, and `phases.md` first — the decisions are **locked**
there; follow them. Keep the strength features working; everything here is additive.

## Scope (R0 only — no live tracking yet, that's R1)

1. **Rebrand to `Ember`.** Change `app_name` (`res/values/strings.xml`), the launcher label, the
   splash title, the Settings footer ("Workout Tracker · 2.x" → "Ember · 2.x"), and the root README
   heading. **Do not** change `applicationId` (`com.lukr99.workout`) — it must upgrade in place.
2. **Shell (locked):** in `ui/App.kt` + `ui/Navigation.kt`:
   - The center **`＋ Start`** opens a small **chooser sheet**: **💪 Lift** (existing
     `createWorkoutSession` → live workout) and **🏃 Run** (→ new live-run flow, stubbed in R0).
   - Add a **`Runs` tab** (new `ui/run/RunsScreen.kt`, placeholder hub) in History's old slot.
   - **Move History into the `Progress` tab** — Progress gains a History/sessions section (reuse the
     existing history list + `WorkoutDetail` navigation); remove History as its own tab.
   - Keep the equal-width floating nav + ember center action; match the design system.
3. **Location plumbing + map:**
   - Just-in-time **fine-location** permission with a rationale (also `POST_NOTIFICATIONS`); manifest
     entries per `architecture.md` (no background-location).
   - Add **MapLibre** (Compose interop) with a **dark vector style**; tiles via **Protomaps
     (PMTiles)** — bundle or point at a small region file for dev. Wrap it in a provider-agnostic
     `ui/run/components/MapView.kt`.
   - A `RunScreen`/`LiveRunScreen` **stub** that renders the dark map and **follows the current
     location** (blue dot / recenter). No recording yet.
4. **Run data model + Room `v5` migration (additive, non-destructive):**
   - `data/run/RunEntities.kt`: `runs`, `run_points`, `routes`, `route_points` (fields per
     `architecture.md`); FKs cascade for child points.
   - `MIGRATION_4_5` adds the four tables; **check in `app/schemas/5.json`**; extend
     `WorkoutMigrationTest` to cover `…→5`. **No destructive fallback.**
   - `data/run/RunRepository.kt` skeleton (the only IO boundary for runs/routes) + a **polyline
     codec** (Google encoded-polyline) with unit tests.
   - `domain/run/RunModels.kt` (`@Serializable` Run/Route/TracePoint/Split) + `domain/run/Pace.kt`
     (pace/speed/distance/split math) — **pure Kotlin, unit-tested**.
   - Bump `ExportBundle` to **1.5** (adds empty `runs`/`routes` arrays; readers still accept 1.0–1.5).
5. Wire a `RunViewModel` + `AppContainer.runRepository` (additive lazy).

## Constraints

- `domain/run/**` stays **Android-free** (unit-tested); location/map/service are `data/`+`ui/`; the
  repository is the only Room/file boundary. Migrations additive + non-destructive.
- Don't regress strength flows; the Lift path through the new chooser must behave exactly as before.
- New deps (MapLibre, Protomaps/PMTiles reader) isolated; keep secrets/keys out of the repo.

## Build / verify (IMPORTANT — current SDK caveat)

The canonical LOCALAPPDATA SDK is **partly on a detached `E:` drive** (`build-tools\35.0.0` +
`platforms\android-35` are junctioned to E:), so builds against it currently fail with "volume not
attached". Two options: **(a)** reconnect `E:` / reinstall `build-tools;35.0.0`+`platforms;android-35`
into `%LOCALAPPDATA%\Android\Sdk`; or **(b)** build against the intact
`C:\Program Files (x86)\Android\android-sdk` (has platform 35 + **build-tools 36.0.0**) by pointing
`local.properties` `sdk.dir` there and locally setting `buildToolsVersion = "36.0.0"` (keep both
changes **uncommitted**; the repo stays pinned to 35.0.0). Then:
`.\gradlew.bat testDebugUnitTest`, `assembleDebug`, and `.\tools\build-and-install.ps1 -Launch` on the
Galaxy A56; `.\tools\phone.ps1 screenshot`. Stale-daemon "SDK location not found" → `.\gradlew.bat --stop`.

## Done when

The app is branded **Ember**; the `＋ Start` chooser offers Lift/Run; a **Runs** tab exists and
**History lives under Progress**; the Run screen shows a dark map following your location; Room
migrates cleanly to **v5** with `5.json` + migration test; `RunRepository`/`domain.run` + polyline
codec are unit-tested; strength flows are unaffected. Install on the A56, screenshot the new shell +
map, and write `docs/run-mode/phase-R0-report.md`. Then R1 (live run core) builds on this.
