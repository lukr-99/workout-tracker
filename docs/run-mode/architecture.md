# Run Mode — Architecture & tech decisions

Extends the existing layered MVVM app (repository is the only IO boundary; `domain/` stays
Android-free; one-file-per-screen Compose). Everything below is additive — no change to the strength
domain.

## Package layout (additive)

```
com.lukr99.workout
├─ data/
│  ├─ run/
│  │  ├─ RunEntities.kt        Room @Entity: runs, run_points, routes, route_points
│  │  ├─ RunDao.kt             Flow queries + upserts; trace read/write
│  │  ├─ RunRepository.kt      only IO boundary for runs/routes; polyline encode/decode
│  │  └─ RunHealthConnect.kt   write ExerciseSessionRecord + ExerciseRoute (reuse data/health)
│  ├─ location/
│  │  ├─ LocationService.kt    foreground service; FusedLocation updates → Flow
│  │  └─ RunTracker.kt         accumulates trace, distance, pace, splits, auto-pause (pure-ish)
│  ├─ map/                     tile/style config, offline cache glue
│  ├─ routing/RoutingClient.kt snap/plan via Valhalla/GraphHopper; elevation
│  └─ music/SpotifyController.kt  App Remote connect + play/pause/skip + current track
├─ domain/run/
│  ├─ RunModels.kt             @Serializable Run, Route, TracePoint, Split (portable)
│  ├─ Pace.kt                  pace/speed/distance math, smoothing, split computation
│  └─ RunStats.kt              aggregates: weekly distance, pace trend, PRs (pure)
└─ ui/run/
   ├─ RunsScreen.kt            hub: recent runs, saved routes, start
   ├─ LiveRunScreen.kt         live map + metrics + controls + Spotify mini-player
   ├─ RunDetailScreen.kt       map, splits, pace/elevation charts
   ├─ RoutePlannerScreen.kt    draw/snap/save routes
   ├─ RunViewModel.kt / LiveRunViewModel.kt / RoutePlannerViewModel.kt
   └─ components/  MapView (MapLibre), SplitTable, PaceChart, ElevationChart, RunMiniPlayer
```

## Data model (Room schema **v5**, additive + non-destructive)

- `runs` — id, optional linked `sessionId`, startedAtUtc, durationSeconds, movingSeconds,
  distanceMeters, avgPaceSecPerKm, elevationGainM, calories?, avgHr?, source, externalKey,
  encodedPolyline (for quick map render), routeId?, notes.
- `run_points` — runId (FK, cascade), t (ms offset), lat, lon, elevationM?, speedMps?, hrBpm?,
  accuracyM. (Raw trace; `encodedPolyline` is the denormalized fast path.)
- `routes` — id, name, distanceMeters, elevationGainM, encodedPolyline, createdAtUtc, notes.
- `route_points` — routeId (FK, cascade), seq, lat, lon, elevationM?.
- `Migration_4_5` adds the four tables; `5.json` checked in; migration test covers `…→5`.
- **Trace storage:** keep raw `run_points` (for re-analysis/GPX) **and** an encoded polyline (Google
  polyline algorithm) on `runs` for fast map thumbnails. Runs optionally link to a `WorkoutSession`
  (EntryType Cardio) so they appear in unified history/volume without duplicating logic.
- `ExportBundle` → **1.5**: adds `runs` + `routes` (readers still accept 1.0–1.5, ignore unknown).

## Live run — location & service

- **Foreground service** (`LocationService`) with `foregroundServiceType="location"` and an ongoing
  notification (distance/time/pace + pause/stop actions). This is what lets GPS keep sampling with the
  screen off.
- **FusedLocationProviderClient** high-accuracy updates (~1s / balanced by speed); Kalman/accuracy
  filtering + min-distance gating in `RunTracker` to reject GPS jitter.
- **Auto-pause** when speed ≈ 0; **splits** at each km/mi with the split pace; **moving vs elapsed**
  time. Everything the UI shows is derived in `RunTracker`/`domain/run` (testable).
- Survives process death mid-run: the service owns the trace and persists incrementally to `runs`/
  `run_points`; the VM re-attaches on return.

## Permissions (request just-in-time, explain first)

- `ACCESS_FINE_LOCATION` (+ `ACCESS_COARSE_LOCATION`), `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_LOCATION` (Android 14), `POST_NOTIFICATIONS`, `ACTIVITY_RECOGNITION` (optional,
  cadence/steps). `ACCESS_BACKGROUND_LOCATION` is **not** required — a visible foreground service
  covers screen-off tracking; avoid it to keep the permission story clean. `HIGH_SAMPLING_RATE_SENSORS`
  only if using barometer for elevation.
- Reuse the existing Health Connect permission flow for writing runs + routes.

## Maps & routing (recommended stack, with alternatives)

- **MapLibre Native (Android) + a Compose wrapper.** Dark vector style matching our palette (ember
  route line, near-black basemap). Renders the live polyline, run-detail trace, and the planner.
  - **Tiles:** **Protomaps `.pmtiles`** (a single vector-tile file, self-hostable or bundled per-region
    for offline; no per-tile key), or **MapTiler** free tier (hosted, needs a key). Keyless-but-raster
    fallback: **osmdroid** with OSM raster tiles (simplest, least slick).
- **Routing/snap for planning:** **Valhalla** or **GraphHopper** (self-host, or a demo/keyed endpoint)
  for road/path snapping + turn geometry + elevation. Live-run tracking needs **no** network.
- **Elevation:** from the routing provider, or a bundled SRTM/terrain source; smooth before summing
  gain.
- **Offline:** cache the map region around a planned route / recent area (PMTiles makes this trivial).
- **Decision hooks:** if you'd rather ship fastest, **Google Maps Compose** + **Maps SDK** is the
  quickest (Play Services present on the device) — the plan's screens are map-provider-agnostic behind
  a thin `MapView` component so the provider can be swapped.

## Music — Spotify

- **Spotify App Remote SDK**: connect to the installed Spotify app, show current track + art, and
  play/pause/skip from the `RunMiniPlayer` on the live screen. Requires a Spotify developer client id
  + redirect URI (registered once) and the Spotify app installed. **No secrets in the repo** — the
  client id is public-ish but keep it in a git-ignored `spotify.properties`/env, same pattern as
  signing.
- **Fallback (if App Remote is too heavy):** a deep-link/`Intent` that just opens Spotify — trivial,
  no SDK, no auth. Chosen at R4 per the "Spotify depth" decision.

## Stats & Health Connect

- `domain/run/RunStats` (pure): weekly/monthly distance, average pace trend, **PRs** (fastest 1k/5k/
  10k/half, longest run, most elevation), consistency — mirrors the strength analytics style; surfaced
  in **Progress** as a Running section (reuse `ProgressChart`/`VolumeBars`).
- **Health Connect:** write each run as `ExerciseSessionRecord` (type Running) with an `ExerciseRoute`
  + distance/energy, keyed by the run's stable fingerprint (idempotent) — extends the existing
  `data/health` service. Optional read/import of runs recorded elsewhere.

## Battery, testing, constraints

- GPS + map are battery-heavy: adaptive sampling, no map redraw when backgrounded, stop updates on
  finish. Document expected drain.
- `domain/run/**` is pure Kotlin (unit-tested: pace, splits, PR detection, polyline codec). Location/
  service/map/routing/Spotify are `data/`+`ui/` (Android). Repository stays the only Room/file boundary.
- Migrations additive + non-destructive (baselines `1..5.json`), never destructive fallback.
- New dependencies are isolated (MapLibre, Spotify App Remote, an HTTP client already present); keep
  the strength build untouched.
