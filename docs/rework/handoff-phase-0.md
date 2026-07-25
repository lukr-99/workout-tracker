# Handoff Prompt — Phase 0: Scaffold the native Android project

> Paste the block below as the opening prompt for the next agent. It is self-contained.

---

You are scaffolding the ground-up rewrite of **Workout Tracker** as a native Android app. The old
`.NET MAUI` app is a frozen proof-of-concept (tag `v1.0.0`, branch `release/1.0`) — **do not edit
it**; it's reference-only. Your work happens on the existing branch **`feature/app-rework`** in
`F:\Code\workout-tracker` (already checked out; commit + push there).

## First, read the plan (it's the spec — follow it, don't reinvent)

- `docs/rework/README.md` — overview + locked decisions
- `docs/rework/00-migration-plan.md` — **your scope is Phase 0 only**
- `docs/rework/01-architecture.md` — the exact package/module structure to create
- `docs/rework/02-design-system.md` — the dark theme tokens, type scale, nav spec
- `docs/rework/03-data-model.md` — (context only; the data layer is Phase 1, not now)

Also clone the reference app and copy its patterns (structure, floating nav, Theme, Gradle setup):
`https://github.com/lukr-99/ring-set.git`. Our app should look and be organised like it.

## Phase 0 goal (from 00-migration-plan.md)

An **empty but well-structured app that builds and installs** — no features yet. Exit check:
**the app installs on the phone, launches, and shows the floating bottom nav over an empty,
dark-themed Home screen.**

## Do exactly this

1. **Gradle project** (Kotlin DSL): root `build.gradle.kts`, `settings.gradle.kts`,
   `gradle/libs.versions.toml` version catalog, `gradlew`/`gradlew.bat` wrapper, `gradle.properties`,
   a single `:app` module. Keep the existing `WorkoutTracker.*` MAUI dirs untouched alongside it.
2. **Dependencies** (via the catalog): Kotlin, Jetpack **Compose** + **Material 3** (BOM),
   **Room**, **DataStore-preferences**, **kotlinx.serialization**, Compose activity/lifecycle,
   `androidx.lifecycle` viewmodel-compose. `minSdk 26`, target/compile the current stable SDK.
3. **App identity**: package/namespace **`com.lukr99.workout`**, applicationId `com.lukr99.workout`
   (debug can use a `.debug` suffix so it coexists with the MAUI build on the phone).
4. **Structure**: create the package tree from `01-architecture.md` — `data/`, `domain/`,
   `settings/`, `ui/` with `theme/`, `components/`, `screens/`. For Phase 0 the screens and
   components are **empty stubs** (`fun HomeScreen(vm)` etc. rendering a title placeholder). No
   Room entities/DAO yet — just the empty packages/files so the shape is real.
5. **Theme** (`ui/theme/Theme.kt`, `Color.kt`, `Type.kt`): implement the dark + light Material 3
   schemes from `02-design-system.md` (ember-orange `#F97316` primary, `bg #0B0D10`,
   `surface #14181D`, etc.), dark as default. Tabular-figure type scale.
6. **Shell** (`ui/App.kt` + `MainActivity.kt`): a `Screen` enum and the **full-width rounded
   floating bottom nav** ported from ring-set's `App.kt` (equal-width tabs, icon + tiny label,
   selected tab tinted `primary @ 16%`). Tabs per `02-design-system.md` screen inventory
   (Home, Workout, Templates, Catalog, History, Stats, Settings). Edge-to-edge; dark status bar.
   Routing just switches between the empty screen stubs.
7. **Build + install on the phone** using the existing tooling — do not hand-roll adb:
   ```powershell
   .\tools\build-and-install.ps1 -Launch
   ```
   The phone is a Samsung Galaxy A56 already authorized for USB debugging; `tools/common.ps1`
   locates adb. Confirm it launches and shows the nav + empty Home. `.\tools\phone.ps1 screenshot`
   to capture proof.
8. **Sanity test**: a trivial JVM unit test (e.g. in `domain/`) so `gradlew test` is wired.

## Constraints

- **Scope discipline**: Phase 0 only. No logging features, no Room schema, no importer — those are
  Phases 1–3. Resist building screens' contents.
- `domain/` must have **no Android imports** (keep it pure Kotlin) — enforced from day one.
- Match ring-set's conventions: one file per screen, reusable visuals in `ui/components/`,
  declarative/stateless screens, repository-as-only-IO (stub the repo for now).
- Keep commits focused; update `docs/rework/README.md`'s status checklist (tick "Scaffold the
  Gradle/Compose project"). Commit and push `feature/app-rework`.

## Done when

`gradlew assembleDebug` succeeds, `build-and-install.ps1` installs it, the app launches to a
dark-themed empty Home with the working floating nav, `gradlew test` is green, and it's committed
and pushed. Then report what's ready and what Phase 1 (data core) should tackle next.
