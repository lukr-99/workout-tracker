# Run Mode (v2.1) — a Strava-style running experience

A first-class **running** experience inside the app: live GPS runs with a map, live pace/distance/
splits, route planning + saving, a run history with maps and stats, and quick in-run Spotify control.
Built on the existing native-Android stack (Kotlin/Compose/Room/MVVM, minimal-dark + ember), it sits
alongside strength training rather than replacing it.

Developed on branch **`feature/run-mode`** (off `v2.0.0`). Ships as **v2.1.0**.

## Vision / scope

- **Live run:** one-tap start → foreground-service GPS tracking → live map with the growing route,
  live distance / duration / current & average pace / splits (km or mi) → pause/resume/auto-pause →
  finish → saved run with full trace.
- **Maps:** a slick dark map (fits our aesthetic) for the live run, run detail, and route planning.
- **Route planning & saving:** draw/plan a route that snaps to roads/paths, see distance + elevation,
  **save** it, start a run that **follows** a saved route (with off-route awareness).
- **Spotify:** easy in-run playback control — current track + play/pause/skip — without leaving the
  run screen (App Remote), with a deep-link fallback.
- **Statistics:** per-run (splits, pace curve, elevation, map) and aggregate (weekly distance, pace
  trend, personal records: fastest 1k/5k/10k, longest run), surfaced in Progress.
- **Interoperability:** write runs (with route) to **Health Connect**; GPX import/export; runs also
  appear in unified history.

## How it fits the shell (decision needed — recommended option below)

Today's bar is `Home · History · (＋ Start) · Progress · Settings`. Run mode needs a home. Options:

- **A (recommended): the center `＋ Start` becomes a mode chooser** (💪 *Lift* / 🏃 *Run*), and a
  **`Runs` tab** replaces `History` in the bar (History folds into Home "recent" + Progress). Bar:
  `Home · Runs · (＋ Start) · Progress · Settings`. Keeps 5 tabs, makes running first-class, and the
  central action starts either activity.
- **B:** add a 6th `Run` tab (denser bar; simplest routing, but 6 items is tight on the label sizes).
- **C:** Run lives entirely under a card on Home + the center chooser (no dedicated tab).

Recommended **A**. Final call is a design decision (see [architecture.md](architecture.md) and the
open decisions below).

## Tech stack (recommended — details + alternatives in architecture.md)

| Concern | Recommended | Why / alternative |
|--------|-------------|-------------------|
| Map | **MapLibre Native + Compose** with a dark vector style | Open-source, no per-request billing, great dark styling. Alt: Google Maps Compose (fast, Play-Services-tied). |
| Tiles | **Protomaps (PMTiles)** or MapTiler free tier | PMTiles = single self-hostable/offline file, no per-tile key. Alt: raster OSM via osmdroid (simplest, less slick). |
| Routing (snap/plan) | **Valhalla or GraphHopper** (self-host or demo endpoint) | Path snapping + elevation. Alt: Mapbox Directions (token). |
| Location | **FusedLocationProvider** + a **foreground service** | Standard for run tracking; battery-aware sampling. |
| Music | **Spotify App Remote SDK** | In-run mini-player; fallback = deep-link to Spotify. |
| Persistence | Room (new `runs`, `route_points`, `routes` tables) | Schema **v5** migration; trace as points + encoded polyline. |
| Health | **Health Connect** `ExerciseSessionRecord` + `ExerciseRoute` | Reuse the existing `data/health` service. |

## Phases (summary — full detail in [phases.md](phases.md))

- **R0 — Foundation:** shell integration, permissions, location plumbing, map rendering, run data
  model + Room v5. *Exit: an empty Run screen shows a live dark map centered on you.*
- **R1 — Live run core:** foreground GPS service, live metrics + splits, growing polyline,
  start/pause/resume/finish, saved run. *Exit: record + save a real run on-device.*
- **R2 — Run detail, history & stats:** per-run map/splits/elevation, runs list, aggregate stats +
  PRs in Progress, Health Connect write. *Exit: past runs render with maps and feed stats.*
- **R3 — Route planning & saving:** draw + snap routes, elevation profile, save, start-from-route +
  off-route awareness. *Exit: plan, save, and follow a route.*
- **R4 — Spotify:** in-run mini-player (track + play/pause/skip), auth, deep-link fallback.
- **R5 — Polish:** auto-pause, audio/haptic split cues, countdown, GPX import/export, offline tile
  caching, share-run card, home-screen widget.
- **R6 — Optional/advanced:** BLE/Health-Connect heart rate, segments, ghost/route racing, cadence.

## Open decisions (confirm before/at R0)

1. **Shell integration** — option A / B / C above (recommended **A**).
2. **Map + tiles provider** — MapLibre+Protomaps (recommended) vs Google Maps Compose (fastest) vs
   osmdroid (keyless raster). Affects look, keys, cost, offline.
3. **Routing provider** — self-hosted Valhalla/GraphHopper vs a hosted demo/keyed endpoint (route
   planning needs one; live run tracking does **not**).
4. **Spotify depth** — full App Remote mini-player vs simple deep-link launch.
5. **Rebrand name** — see [../rebrand-candidates.md](../rebrand-candidates.md).
