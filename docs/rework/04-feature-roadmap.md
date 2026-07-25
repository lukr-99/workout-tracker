# Feature Roadmap

Everything a serious modern workout tracker should do, sequenced. Milestones map onto the phases
in [00-migration-plan.md](00-migration-plan.md). Items marked ⟳ are carried from the MAUI
`docs/roadmap.md` open ideas; the rest are new.

## M1 — Parity (must match 1.0 POC)

The rework isn't "done" until it does everything the MAUI app did, better:

- Exercise catalog: browse, search, filter (body part, category, archived), custom exercises,
  archive-not-delete.
- Templates: create/edit/reorder, start a session from a template.
- Live workout: from scratch or template; add/remove exercises; inline strength sets
  (reps × kg, RIR/RPE); cardio (duration, distance, calories); session duration/timer;
  finish / discard with confirmation.
- History: list + filters (body part, date range, type), detail view, **edit completed workouts**.
- Duplicate / repeat previous workout ⟳.
- Home summaries: recent workouts, best set, total volume, trend blurbs.
- Persisted theme (system/light/dark) + toast confirmations.
- JSON + CSV export.

## M2 — Data freedom

- **Lyfta CSV import** (see [05-lyfta-import.md](05-lyfta-import.md)).
- `v1.0.0` JSON import (nothing lost from the POC).
- Full backup/restore via `ExportBundle` (Storage Access Framework: save/open document) ⟳.
- Share sheet export.
- Import conflict handling / dedupe ⟳.

## M3 — Progression & insight (the Lyfta-grade payoff)

- **Exercise progression charts** ⟳: best set, **estimated 1RM** (Epley/Brzycki) ⟳, total volume
  over time, with scrub + range.
- **Personal records & milestones** ⟳: per-exercise 1RM/rep-max PRs, volume PRs, PR feed, PR
  celebration on log.
- **Consistency**: workout frequency, current/longest streak ⟳, weekly & monthly views ⟳.
- **Volume analytics**: weekly tonnage, per-body-part volume, **muscle heatmap** distribution ⟳.
- Bodyweight logging + trend; volume-per-bodyweight.
- Set/rep/volume trend summaries per exercise.

## M4 — Live-session power

- **Rest timers** ⟳: auto-start on set completion, per-exercise defaults, +15s/skip, notification
  + haptic + optional sound; keeps running in the background (foreground service).
- **Supersets & grouped exercises** ⟳.
- **Warm-up sets** (excluded from volume/PRs).
- Plate calculator (target weight → plates per side, configurable bar + plate inventory).
- Previous-performance hints inline (last time you did this exercise: reps × kg).
- Auto-fill next set from the last set; quick "same as last" .
- Notes per workout, per exercise block ⟳, and per set.
- 1RM / e1RM shown live as you log.
- Session RPE (perceived effort) at finish.
- Screen-on / keep-awake during a workout.

## M5 — Planning & structure

- **Routines / programs**: multi-day split (e.g. Push/Pull/Legs), schedule, "today's workout".
- Progressive-overload suggestions (target next weight/reps from history).
- Deload / weekly volume targets per muscle group.
- Exercise substitutions (swap with a similar movement, keep history linked).
- Calendar view of planned vs done.
- Weekly review / recap.

## M6 — Ecosystem & platform

- **Health Connect** integration (read bodyweight, write workouts/active energy).
- **Wear OS** companion for logging from the wrist (long-term).
- Home-screen **widgets** (today's routine, streak, quick-start).
- Quick-settings tile / shortcut to start a workout.
- **Windows/desktop data tool** (separate project) consuming the `ExportBundle` — the phone stays
  the source of truth; desktop is read/analyze first (see architecture portability seam).
- Optional cloud sync / multi-device (much later; only if wanted — the format already supports it).

## M7 — Content & discovery

- Richer seeded catalog with categories, equipment, primary/secondary muscles.
- Exercise instructions / images (respect licensing; `wger` remains a candidate source ⟳).
- Exercise demo GIFs/diagrams (bundled or fetched, licensing permitting).
- Search improvements: recent, favorites, most-used.

## M8 — Refinement

- **Units**: kg/lb toggle, distance mi/km, plate units — throughout.
- Localization scaffolding (strings already externalized).
- Accessibility: TalkBack labels, large-text, contrast, reduce-motion honored.
- Onboarding for first run + import.
- Data hygiene: merge duplicate exercises, bulk edit, archive management.
- Reminders / notifications (workout day, rest-day nudge) — opt-in.

## Someday / maybe

- Social: share a workout card / PR image.
- Apple Health / Strava-style export targets.
- AI form/notes assistant (on-device or opt-in).
- Nutrition hooks (out of scope for a *tracker*, but a common integration ask).

## Non-goals (keep it focused)

- No account/login requirement — offline-first, local-first stays the default.
- No ads, no paywalled core logging.
- Not a general fitness/social network — it's a fast, beautiful **lifting logger** first.
