# Design System — Minimal Dark (Lyfta-inspired, our own spin)

The look is **calm, dark, and dense-but-breathable**: near-black surfaces, one energetic accent,
generous spacing, big legible numbers, and motion that rewards logging. We borrow Lyfta's
restraint (no gradients-for-the-sake-of-it, no card clutter) and keep our own **ember-orange**
accent for continuity with the 1.0 POC — fitting for a lifting app.

> This is a spec, not final CSS. Values become tokens in `ui/theme/Color.kt` and `Type.kt`.

## Principles

1. **Dark is the product, not a mode.** Light theme exists and is supported, but every screen is
   designed dark-first.
2. **One accent, used sparingly.** Ember-orange marks the primary action / current focus and
   nothing else competes with it. Data gets its own quiet per-metric hues.
3. **Numbers are the hero.** Weights, reps, volume, PRs — large, tabular, high-contrast.
4. **Surfaces over cards.** Prefer flat sections separated by spacing and hairline dividers over
   heavy elevated cards. Elevation is a whisper (tonal, low shadow).
5. **Motion confirms.** Every logged set, finished workout, and new PR gets a small, fast,
   physical animation. Never gratuitous, never slow.

## Color tokens (dark)

| Token | Hex | Use |
|-------|-----|-----|
| `bg` | `#0B0D10` | App background (true near-black). |
| `surface` | `#14181D` | Sections, sheets, nav bar. |
| `surfaceHigh` | `#1B2027` | Raised rows, active set, menus. |
| `outline` | `#232A32` | Hairline dividers, input borders. |
| `textHi` | `#F2F5F8` | Primary text, numbers. |
| `textMid` | `#9AA4B0` | Labels, secondary text. |
| `textLo` | `#5C6672` | Hints, disabled, timestamps. |
| `primary` (ember) | `#F97316` | Primary action, current focus, FAB. |
| `primaryPress` | `#FB8B3D` | Pressed/hover tint of primary. |
| `onPrimary` | `#160B03` | Text/icon on the accent. |
| `positive` | `#34D399` | PRs, gains, success. |
| `warning` | `#FBBF24` | RPE-high, caution. |
| `danger` | `#F26D6D` | Destructive confirm (discard/delete). |

### Per-metric / per-body-part accents

Kept quiet and distinct from the ember primary, so charts and tags never fight the action color
(borrows ring-set's metric-color idea):

| Meaning | Hex |
|---------|-----|
| Strength volume | `#F97316` (ember) |
| Cardio | `#22D3EE` (teal) |
| e1RM / strength trend | `#A78BFA` (violet) |
| Reps / count | `#34D399` (green) |
| Chest | `#F472B6` · Back `#60A5FA` · Legs `#34D399` · Shoulders `#FBBF24` · Arms `#A78BFA` · Core `#22D3EE` |

### Light theme

Inverts to `bg #F4F6F9`, `surface #FFFFFF`, `textHi #0C1220`, same ember primary at a slightly
deeper `#E4670C` for contrast. Both schemes live in `Theme.kt`; the viewer's system setting +
an in-app override (DataStore) pick between them.

## Typography

- **Family:** system default (Roboto) for body; a **tabular-figures** style for all numbers so
  columns of weights/reps align. Consider `Inter`/`Manrope` bundled later — start with system.
- Scale (sp): Display 34 · Title 22 · Section 17 (semibold) · Body 15 · Label 13 · Caption 11.5.
- **Numbers:** use `FontFeatureSetting("tnum")`-equivalent / monospaced digits in set rows,
  charts, and stat tiles. Weights render with the unit as a small `textMid` suffix.

## Spacing & shape

- Base unit **4dp**; screen gutters **18dp** (matches ring-set). Section gap 16–20dp.
- Corner radii: inputs/rows 12dp · sections 16dp · sheets 24dp · nav bar 26dp · pills full.
- Hairline dividers `outline` at 1dp, used instead of boxing everything in cards.
- Minimum touch target 44dp; set-row steppers are 44dp hit areas.

## Core components (→ `ui/components/`)

- **FloatingNav** — full-width rounded bar pinned bottom, equal-width tabs, icon + tiny label,
  selected tab tinted with `primary @ 16%` behind the icon. (Ported from ring-set `App.kt`.)
  **Decided shell (resolves phase-0-report §4.2):** **5 items with a central Start action** —
  `Home · History · (＋ Start) · Progress · Settings`. The center `＋` is a raised **ember**
  Start/Resume-workout button (the primary action; container-transform into the live session),
  *not* a peer tab. **Templates + Catalog are not tabs** — they live in a **Library** surface
  reached from Home. Mirrors Lyfta's `Home · Explore · ＋Workout · Progress · You` bar (we drop
  Explore and repoint the center action straight at logging). See
  [06-lyfta-study.md](06-lyfta-study.md).
- **ScreenHeader** — big title + muted subtitle + optional info affordance. Every screen uses it.
- **StatTile** — label (caption, `textMid`) over a big tabular number; optional delta chip in
  `positive`/`danger`. Used in Home + Stats KPI rows.
- **SetRow** — the workhorse: `set # · reps [stepper] × kg [stepper] · RIR/RPE · ✓`. Active set
  gets `surfaceHigh`; completed sets dim slightly; a new PR pulses `positive`.
- **NumberStepper** — editable numeric field with −/+ and long-press repeat; the reusable
  "editable suggestion input" the MAUI roadmap wanted.
- **ProgressChart** — Canvas line/area chart with a scrub handle + range brush (ring-set
  `MetricChart` lineage). Used for e1RM, volume, bodyweight.
- **VolumeBars** — weekly volume / session-frequency bars with a "this week" highlight.
- **BodyHeatmap** — optional front/back muscle map tinted by recent volume per body part.
- **RestTimerBar** — sticky bottom surface above the nav during a live workout: countdown ring,
  +15s / skip, auto-starts on set completion.
- **Chips** — filter chips (body part, type, date range), choice chips, body-part tags.
- **Toast** — transient top notification for confirmations (replaces MAUI inline status blocks).

## Motion

- **Set logged:** checkmark springs in; row settles to "done" in ~180ms.
- **PR hit:** number counts up + a brief `positive` glow; subtle haptic.
- **Screen change:** cross-fade + 12dp vertical slide, ~220ms, standard easing.
- **Rest timer:** ring drains smoothly; last 3s pulse; gentle haptic + optional sound at zero.
- **FAB → live workout:** container-transform-style expand where feasible.
- Respect `Settings > reduce motion` and the system animator-duration scale.

## Screen inventory (dark-first)

Grouped by the decided 5-item shell:

- **Home** (dashboard) — today, active/resume, quick-start, recent; hosts the **Library** entry
  (→ `Templates` + `TemplateEditor`, `Catalog` + `ExerciseEditor`).
- **History** (+ `WorkoutDetail`) — past sessions list + drill-in.
- **＋ Start** (center action) — begins/resumes the live **`Workout`** (logging) screen; not a tab.
- **Progress** — the analytics home (Lyfta's standout): overview stat cards + muscle map, a
  per-exercise list with sparklines, and per-exercise detail (e1RM + volume). Supersedes the old
  flat `Stats` tab.
- **Settings**.

Secondary/full-screen flows off these: `Library` (Templates + Catalog), `TemplateEditor`,
`ExerciseEditor`, `WorkoutDetail`, per-exercise `ProgressDetail`. Longer editing flows are full
screens; dialogs are reserved for confirmations and one-shot picks (carried over from the MAUI UI
direction).

## Reference

We're following Lyfta's *feel* (minimal, dark, data-forward), not copying its layout or brand.
Reference frames from Lyfta's Progress tab are captured (git-ignored) under
`docs/rework/reference/lyfta/` as private mood-board input — do not ship its assets. Findings and
the component vocabulary they inform are written up in [06-lyfta-study.md](06-lyfta-study.md).
