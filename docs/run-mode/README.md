# Run Mode (v2.1) — a Strava-style running experience

A first-class **running** experience inside the app: live GPS runs with a map, live pace/distance/
splits, route planning + saving, a run history with maps and stats, and quick in-run Spotify control.
Built on the existing native-Android stack (Kotlin/Compose/Room/MVVM, minimal-dark + ember), it sits
alongside strength training rather than replacing it.

Developed on branch **`feature/run-mode`** (off `v2.0.0`). Ships as **v2.1.0**.

## Decisions (LOCKED)

1. **App name → `Ember`** (rebrand; keeps the ember dumbbell logo; `applicationId` unchanged). See
   [../rebrand-candidates.md](../rebrand-candidates.md). Wired in during R0.
2. **Shell:** the center **`＋ Start` becomes a chooser** (💪 Lift / 🏃 Run). A **`Runs` tab replaces
   `History`**; **History moves into the Progress tab**. Bar: `Home · Runs · (＋ Start) · Progress ·
   Settings`.
3. **Map:** MapLibre Native + Compose, dark vector style; **tiles: Protomaps (PMTiles)** (offline-
   friendly, keyless). Screens stay provider-agnostic behind a `MapView` component.
4. **Routing:** **Valhalla** (foot/pedestrian profile + elevation + **map-matching** to snap traces
   to paths — the most rigid/useful for planning *and* cleaning GPS). GraphHopper is the fallback.
   Only route *planning*/snapping needs it; live tracking is fully offline.
5. **Music (Spotify): minimal, and shared with strength workouts.** A small `MusicMiniControls`
   component: **open Spotify** + (when Spotify is connected) **play/pause · next · previous** + current
   track. Used on **both** the live-run screen **and** the live strength-workout screen. No full
   in-app player. Live-run priority remains map + pace + start/pause/resume.

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

## Shell (locked)

Bar becomes `Home · Runs · (＋ Start) · Progress · Settings`:
- **`＋ Start`** opens a small chooser sheet: **💪 Lift** (existing live workout) or **🏃 Run** (new
  live run).
- **`Runs`** is the new tab (recent runs + saved routes + start). It takes History's slot.
- **History moves into the `Progress` tab** (a History/sessions section alongside the analytics), so
  no tab is lost and Progress becomes the "look back" hub.

## Tech stack (recommended — details + alternatives in architecture.md)

| Concern | Recommended | Why / alternative |
|--------|-------------|-------------------|
| Map | **MapLibre Native + Compose** with a dark vector style | Open-source, no per-request billing, great dark styling. Alt: Google Maps Compose (fast, Play-Services-tied). |
| Tiles | **Protomaps (PMTiles)** or MapTiler free tier | PMTiles = single self-hostable/offline file, no per-tile key. Alt: raster OSM via osmdroid (simplest, less slick). |
| Routing (snap/plan) | **Valhalla** (foot profile + elevation + map-matching) | Rigid/flexible for planning *and* snapping GPS traces. Fallback: GraphHopper. |
| Location | **FusedLocationProvider** + a **foreground service** | Standard for run tracking; battery-aware sampling. |
| Music | **Spotify App Remote** — minimal `MusicMiniControls` | open Spotify + play/pause/next/prev when connected; **shared by live-run AND live-lift**. |
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

## Decisions — all resolved

See the **Decisions (LOCKED)** section at the top. Name = **Ember**; shell = Runs tab + Start chooser,
History → Progress; map = MapLibre + Protomaps; routing = Valhalla; Spotify = minimal shared controls.
The only remaining setup choice is **where Valhalla runs** (self-hosted vs a hosted endpoint) — needed
at **R3** (route planning), not before, so R0–R2 are unblocked.
