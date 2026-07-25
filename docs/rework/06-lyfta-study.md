# Lyfta Capability Study — backlog to fold into the roadmap

Lyfta is the app the user trains with today and the **visual/UX north star** for the rework
(minimal dark, "our own spin"). This doc is a **backlog of Lyfta capabilities to study hands-on**
and decide whether/how to adopt. It is not a spec — each confirmed item graduates into
[04-feature-roadmap.md](04-feature-roadmap.md) with a milestone.

> How to study: the phone is USB-tethered and Lyfta is installed. Walk each area in the app and
> capture what it does well (and its rough edges) — `.\tools\phone.ps1 screenshot` grabs frames
> while navigating. Confirmed facts so far come from its **CSV export**
> (see [05-lyfta-import.md](05-lyfta-import.md)): per-set rows with `Set Type`
> (normal/warmup/drop/failure/negative/back-off), timed sets, supersets, kg, session
> title+duration. So the data model already implies several of the features below.

## Areas to evaluate

### Logging loop (highest priority — this is the app's core)
- [ ] Live-session ergonomics: how fast is set entry? steppers vs. keypad; "same as last time" prefill.
- [ ] **Set types** in the UI (warmup/drop/failure/negative/back-off) — already in our data model
      (`SetType`); confirm how Lyfta surfaces/enters them.
- [ ] **Supersets / grouped exercises** — export has `Superset id`; see how grouping is created + shown.
- [ ] **Rest timer**: auto-start on set complete, per-exercise defaults, notification, quick adjust.
- [ ] **Plate calculator** / warmup-set calculator.
- [ ] Previous-performance inline reference (last session's weights next to the input).
- [ ] Timed / isometric sets (planks) — export uses `Time`; confirm entry UX.

### Exercise catalog
- [ ] Exercise database size + quality; **animations/illustrations** and **muscle maps** per exercise.
- [ ] Search + filtering (equipment, body part, category); favorites; custom-exercise creation UX.
- [ ] Body-part / muscle taxonomy (ours is a simple primary + secondary list today).

### Templates / routines / programs
- [ ] Routines vs. one-off workouts; folders/mesocycles; scheduled program days.
- [ ] Progression schemes (auto weight/rep progression, double progression).

### Analytics & progress — the **Progress tab** (user-flagged as Lyfta's standout; study closely)
Per the user, Lyfta's **Progress tab is rich and well-designed** — the strongest area to mine for
both features *and* component inspiration. It has (at least) three levels:
- [ ] **General overview** — top-level dashboard: totals, trends, consistency, distribution.
- [ ] **Per-exercise graph** — pick an exercise, see its progression chart on its own.
- [ ] **Per-exercise detail inspect** — drill into a single exercise: history, PRs, breakdowns.
Capture the component vocabulary here (chart styles, stat tiles, selectors, drill-in transitions) —
it feeds `ui/components/` (`ProgressChart.kt`, `StatTile.kt`, `VolumeBars.kt`, `BodyHeatmap.kt`) and
`02-design-system.md`. Specifics to note:
- [ ] Per-exercise progression charts; **estimated 1RM** trend; volume/tonnage over time.
- [ ] **Muscle/body-part distribution** (weekly heatmap) — informs `BodyHeatmap.kt`.
- [ ] Consistency: streaks, calendar heatmap, weekly frequency, PR feed.
- [ ] Personal records surfacing (per rep-range, e1RM, volume).
- [ ] Bodyweight / measurements tracking; progress photos.

> **Out of scope for now:** Lyfta's **Explore tab** — the user asked to ignore it. Don't spend
> study time there.

### Data & platform
- [ ] **Health Connect** integration (Lyfta syncs to it) — a possible richer import path + output.
- [ ] Import/export formats it supports (it imports from other apps → confirms tabular interop).
- [ ] Units handling (kg/lb), plate/bar config, locale.

### Visual / interaction language (design fidelity)
- [ ] Motion: transitions into the live session (container transform?), set-complete feedback.
- [ ] Empty states, charts styling, card/spacing rhythm, accent usage — feed
      [02-design-system.md](02-design-system.md).
- [ ] Haptics + sound on set complete / rest end.

## Confirmed by hands-on capture (2026-07-25)

First pass driving Lyfta over adb. Reference frames saved (git-ignored) under
`docs/rework/reference/lyfta/`: `progress-overview.png`, `progress-exercises.png`,
`exercise-detail-progress.png`.

**App shell / nav (resolves report §4.2):** Lyfta's bottom bar is **5 destinations with a central
action** — `Home · Explore · [＋ Workout] · Progress · You`. The "＋ Workout" (start a session) is a
distinct center item, not a peer tab. This is strong evidence for our shell: **~4–5 tabs + a central
ember "Start workout" action**, dropping the current flat 7-tab bar. (Their `Explore` = discovery
content — the user asked us to ignore it, so our center action goes straight to the live session.)

**Progress tab is genuinely the standout.** Structure:
- Sub-tabs: **Overview · Exercises · Measures · Photos**.
- **Overview**: a swipeable **stat-card carousel** (Workouts count with area-line chart + "▲116% vs
  previous 3 mo." comparison; Volume kg next), then a **Muscle Recovery** card — a row of anatomical
  body figures with per-muscle "ready to train" %/recovery, driven by recent training. Great model
  for our `BodyHeatmap`/recovery surface.
- **Exercises**: searchable list, sorted "Recent Performed"; each row = exercise illustration + name +
  **`Est 1RM. NNkg`** + green **trend delta** (↗8kg) + an **inline e1RM sparkline**. Excellent compact
  component — maps to a `ProgressChart` mini + `StatTile`.
- **Exercise detail** (tap a row): its own sub-tabs **About · History · Progress · Records ·
  Leaderboard**. Progress shows an **Estimated Strength** smooth **spline area chart** (gradient fill,
  point markers, tight y-range like 79–84kg) + a **Workout Volume bar chart** (last/partial bar
  dimmed). "Records" + "Leaderboard" are their own features to consider later.

**Component vocabulary to mirror in `ui/components/`:** stat card w/ big number + period comparison
delta; horizontally-paged card carousel; smooth spline area chart with markers; volume bar chart with
dimmed current bar; inline row sparkline; anatomical muscle map with per-muscle fill. All fit the
minimal-dark system (near-black bg, one accent per chart) in `02-design-system.md` — Lyfta uses a
blue chart accent; we keep **ember**.

**Design taste:** big bold numbers, generous card padding, one saturated accent on an otherwise
monochrome dark canvas, anatomical illustrations for exercises + muscles. Matches our north star.

## Output of this study
1. Tick the confirmed items, note the "our spin" decision per area.
2. Promote adopted features into `04-feature-roadmap.md` with milestones.
3. Capture any schema implications back into `03-data-model.md` (as additive fields).
4. Feed visual details into `02-design-system.md`.

Not blocking any build phase — best done alongside Phase 2 (the logging loop), so what we learn
about Lyfta's live-session UX directly informs the screen we're designing.
