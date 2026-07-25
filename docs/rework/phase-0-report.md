# Phase 0 Report — Scaffold review

> **For:** the main design agent reviewing the native-Android rework.
> **From:** the Phase 0 scaffold agent.
> **Branch:** `feature/app-rework` · **Commits:** `4c9ff55` (scaffold), `435b5f5` (tooling fixes).
> **Date:** 2026-07-25 · **Spec followed:** `docs/rework/00`–`02`, mirroring `lukr-99/ring-set`.

---

## 1. TL;DR

Phase 0 is **complete and verified on real hardware**. The app builds, installs, launches, and
shows the full-width floating bottom nav over an empty, dark-themed Home — the exit check from
`00-migration-plan.md`. `gradlew test` is green. Nothing here has features yet by design; this
review is about **structure and design fidelity**, and about signing off a handful of **decisions**
(Section 4) before Phase 1+ builds on them.

Proof screenshot: captured on the Galaxy A56 (`tools/phone.ps1 screenshot`). The nav renders all 7
tabs with Home ember-tinted; Home shows the display-size title + muted subtitle over `#0B0D10`.

---

## 2. What was built (against the handoff's 8 steps)

| # | Handoff step | Status | Notes |
|---|--------------|--------|-------|
| 1 | Gradle Kotlin-DSL project (root, settings, catalog, wrapper, `:app`) | ✅ | Wrapper = Gradle 8.9 (copied from ring-set). MAUI dirs untouched. |
| 2 | Deps via catalog (Compose+M3 BOM, Room, DataStore, serialization, lifecycle) | ✅ | `gradle/libs.versions.toml`. `minSdk 26`, compile/target 35. |
| 3 | Identity `com.lukr99.workout` | ✅ | **Dropped the `.debug` suffix** — see 4.1. |
| 4 | Package tree + empty stubs; `domain/` pure | ✅ | `data/ domain/ settings/ ui/{theme,components,screens}`. |
| 5 | Theme (dark default) from `02-design-system.md` | ✅ | Tokens in `Color.kt`, scale in `Type.kt`, schemes in `Theme.kt`. |
| 6 | Shell: `Screen` enum + floating nav; edge-to-edge; dark status bar | ✅ | Nav ported from ring-set `App.kt`; 7 tabs. See 4.2–4.4. |
| 7 | Build + install on phone via `tools/build-and-install.ps1 -Launch` | ✅ | `BUILD SUCCESSFUL`; installed + launched; screenshot captured. |
| 8 | Sanity JVM unit test so `gradlew test` is wired | ✅ | `domain/UnitsTest` (kg⇄lb). |

---

## 3. Design-system fidelity — what to eyeball

Mapped from `02-design-system.md` into `ui/theme/`:

- **Palette (dark, default):** `bg #0B0D10`, `surface #14181D`, `surfaceHigh #1B2027`,
  `outline #232A32`, text hi/mid/lo `#F2F5F8 / #9AA4B0 / #5C6672`, primary ember `#F97316`
  (`onPrimary #160B03`), plus `positive/warning/danger` and the per-metric/body-part accents.
  → `Color.kt`. These land on M3 roles in `Theme.kt` (`primary`=ember, `background/surface`=near-black
  tiers, `surfaceVariant`=raised, `onSurfaceVariant`=mid/label text, `outline`=hairline).
- **Light theme:** implemented (`bg #F4F6F9`, `surface #FFFFFF`, deeper ember `#E4670C`) but
  **unverified visually** — the app was only exercised in dark. Worth a design pass later.
- **Type scale:** Display 34 / Title 22 / Section 17 / Body 15 / Label 13 / Caption 11.5, mapped to
  M3 `display/title/body/label` slots (`Type.kt`). A `Numbers` style with `tnum` (tabular figures)
  exists for future set-rows/charts but isn't used on screen yet.
- **Floating nav:** full-width rounded (26dp), `surface @ 94% alpha`, tonal+shadow elevation,
  equal-width columns, selected icon on a `primary @ 16%` circle, ember label. Icon 20dp, label 9.5sp.

---

## 4. Decisions that want design sign-off

These are the judgment calls made to keep Phase 0 moving. None are hard to reverse; flagging them so
the design direction is deliberate.

### 4.1 Dropped the debug `applicationId` suffix
The handoff *allowed* a `.debug` suffix "so it coexists with the MAUI build." The frozen MAUI app
ships as `com.lukr99.workouttracker` (a different id), so `com.lukr99.workout` already coexists, and
`build-and-install.ps1 -Launch` targets the un-suffixed id. Keeping the suffix would have broken the
provided tooling's launch step. **No design impact** — noted for completeness.

### 4.2 Tab set, order, and whether "Workout" belongs in the nav — **needs a design call**
Current nav (left→right): **Home · Workout · Templates · Catalog · History · Stats · Settings** — the
7 destinations from the `02` screen inventory, one tab each. Two things to decide:
- **Seven tabs is dense.** It fits on the A56 (see screenshot), but labels are 9.5sp and tight. Lyfta
  and most logging apps keep the bar to ~4–5 destinations.
- **"Start a workout" is arguably a primary action, not a peer tab.** `02-design-system.md` even calls
  for a **FAB → live workout** (container-transform). A common pattern: 4–5 tabs + a central ember FAB
  for the live session, with Templates/Catalog reached from Home or a secondary surface. **This is the
  biggest open design question and it shapes the shell** — worth resolving before Phase 2 (logging loop).

### 4.3 Iconography — provisional
Tab icons are Material rounded: `Dashboard, FitnessCenter, ListAlt, ViewList, History, ShowChart,
Settings` (the list/chart ones switched to `AutoMirrored` to clear deprecation warnings). These are
placeholders picked for legibility — swap freely to match the intended visual language.

### 4.4 System bars: edge-to-edge with **visible** dark bars (not ring-set's immersive hide)
Per the handoff ("edge-to-edge; dark status bar"), `MainActivity` uses `enableEdgeToEdge()` with a
transparent, light-icon status bar and pads content via `systemBars` insets. ring-set instead *hides*
the bars entirely for a full-bleed look. Confirm which you want — this affects top spacing and the
Home title's position.

### 4.5 Screen stubs: only the 7 nav tabs exist
The editor/detail flows named in `01-architecture.md` (`TemplateEditor`, `ExerciseEditor`,
`WorkoutDetail`) are **not** stubbed yet — they arrive with their flows in Phase 2. If you'd rather see
the full screen inventory as empty files now, that's a quick add.

### 4.6 Slightly-beyond-empty content
To make the shell read intentionally rather than blank, three small real pieces were added: a
`ScreenHeader` component, a `StubScreen` placeholder body, and `domain/Units.kt` (so the sanity test
tests real code). Everything else is genuinely a stub. Flag if you want Phase 0 stripped even barer.

---

## 5. Verification evidence

- `tools/build-and-install.ps1 -Launch` → `BUILD SUCCESSFUL in 7m 24s` (first build; deps downloaded),
  APK installed via streamed install, app launched.
- `tools/phone.ps1 screenshot` → valid PNG of the running Home (floating nav + empty dark screen).
- `gradlew test` → `BUILD SUCCESSFUL`; `testDebugUnitTest` + `testReleaseUnitTest` executed, green.
- Device: **Samsung Galaxy A56 (SM-A566B, serial RZCY60P9EHB)**, USB-debugging authorized.

---

## 6. Where things live (review map)

```
build.gradle.kts · settings.gradle.kts · gradle/libs.versions.toml · gradle.properties · gradlew(.bat)
app/build.gradle.kts · app/src/main/AndroidManifest.xml · app/src/main/res/{values,drawable,mipmap-*}
app/src/main/java/com/lukr99/workout/
  MainActivity.kt                      edge-to-edge Compose host
  data/WorkoutRepository.kt            stub (Phase 1/3 owns Room + import/export)
  domain/Units.kt                      pure Kotlin (no Android imports) — tested
  settings/SettingsStore.kt            DataStore delegate stub
  ui/App.kt                            Screen enum + FloatingNav + routing   ← main shell
  ui/WorkoutViewModel.kt               empty VM (no-arg, default factory)
  ui/theme/{Color,Type,Theme}.kt       ← design tokens live here
  ui/components/ScreenHeader.kt        first reusable visual
  ui/screens/{Home,Workout,Templates,Catalog,History,Stats,Settings}Screen.kt + StubScreen.kt
app/src/test/java/com/lukr99/workout/domain/UnitsTest.kt
```

---

## 7. Known issues / tech debt (carry into Phase 1)

- **kapt on Kotlin 2.0** warns it falls back to language version 1.9. Harmless now (no annotations
  processed). When Room entities land, **switch Room to KSP** to drop kapt.
- **AGP 8.5.2 vs compileSdk 35**: AGP warns it's only tested to 34 (build works). Bump AGP when
  convenient; a newer AGP also wants JDK 17+ (already installed).
- **Light theme unverified** visually (see 3).
- Nav is an in-app `Screen` enum (per spec) — fine until per-tab back-stacks/deep links are needed,
  then adopt Navigation-Compose (already a dependency).

---

## 8. Environment note (not committed)

This machine had **no Android SDK / adb / JDK 17** at the start; they were installed with the user's
approval. Gradle uses a local **JDK 17** via `~/.gradle/gradle.properties` while the default
`JAVA_HOME` stays JDK 11. Details (paths, and two PowerShell-5.1 tooling fixes made to
`tools/*.ps1`) are in the **git-ignored `tools/env-local.md`**. Not needed to review the design, but
relevant if the reviewer rebuilds locally.

---

## 9. Suggested review checklist

- [ ] **Shell shape:** confirm 7 tabs vs. fewer-tabs-plus-FAB for live workout (4.2) — highest-impact.
- [ ] Tab **order + iconography** (4.3).
- [ ] **System-bar treatment**: visible dark bars vs. immersive hide (4.4).
- [ ] **Palette + type** on-device match the intended Lyfta-grade dark feel (Section 3); nod on the
      light theme values or defer.
- [ ] Whether to **stub the editor/detail screens** now (4.5).
- [ ] Anything to strip back / add before Phase 1 (data core) starts.
