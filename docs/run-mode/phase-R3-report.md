# Run Mode — Phase R3 report (Route planning — core slice)

**Branch:** `feature/run-mode` · **Ships toward:** v2.1.0 · **Verified on:** Galaxy A56 (`SM-A566B`, Android 16)

R3's core landed: **plan a route by tapping the map, snap it to roads/paths, and save it.** Additive;
strength + R0–R2 flows untouched. Starting a run *from* a saved route (with off-route awareness) and
offline tile caching are the remaining R3 slices (below).

## What shipped

### Routing foundation — `data/routing/`
- `RoutingClient` (interface) + `OsrmRoutingClient` — the only class that talks to a routing provider,
  so it can be swapped without touching the planner. **R3 dev uses the keyless public OSRM demo**
  (`router.project-osrm.org`, `foot` profile) — matching the map tiles' "keyless for dev" pattern;
  swap `baseUrl` for a self-hosted OSRM/Valhalla in production. HTTP via the existing OkHttp; requests
  the `full` geometry as a Google polyline.
- `OsrmRouteParser` (pure, **3 unit tests**) — decodes the OSRM response (code/geometry/distance) into
  a `SnappedRoute` using the existing `Polyline` codec, with a haversine distance fallback and safe
  rejection of non-`Ok`/empty/garbage responses. `AppContainer.routingClient` wired.

### Planner UI — `ui/run/`
- `RoutePlannerViewModel` — accumulates tapped waypoints, **re-snaps after every edit** (cancelling
  the in-flight request), exposes the snapped line + distance + a snapping flag, and saves the snapped
  polyline as a `Route` via `RunRepository.saveRoute`.
- `RoutePlannerScreen` — tap the map to drop waypoints; the snapped **ember route line** + waypoint
  markers render live; a top chip shows distance / snapping state; **Undo / Clear** and a name +
  **Save** dialog. `RunMap` gained `onMapTap` (map click → lat/lon) and a **waypoint circle layer**.
- The Runs hub gained a **Plan a route** entry and a **Saved routes** section (name + distance),
  reactive on the routes flow.

## On-device verification (A56)

| Planner — snapped to roads | Saved in the hub |
|---|---|
| ![planner](r3-screens/planner-snapped.png) | ![saved](r3-screens/saved-routes.png) |

Confirmed on the phone:
- Tapping three points dropped waypoint markers and the **ember line snapped to the real road
  network** (not straight segments) via keyless OSRM, with **0.82 km** computed.
- **Save** persisted the route; it appears under **Saved routes** (`Route · 0.82 km`) in the hub.
- Unit tests green: `OsrmRouteParser` (3) plus the existing run suites; `assembleDebug` builds.

## Remaining in R3 (next slices)
- **Start a run from a saved route**: show the planned line under the live trace, with an **off-route**
  distance indicator / cue. (Plumbing exists — `Run.routeId`, `Route.encodedPolyline`, `RunMap` can
  draw a second line; the live screen needs a "planned route" underlay + a distance-from-route calc in
  `domain/run`.)
- **Saved-routes management**: open a saved route (detail/preview), rename, delete; start-from-route
  entry point.
- **Offline tile caching** for a route's region (MapLibre offline manager) — pairs with the R5 offline
  work.
- **Elevation profile** for a planned route (the routing provider's elevation or a terrain source);
  currently `Route.elevationGainM` is 0 for planned routes.

## Notes
- Routing needs network (OSRM demo); the live run itself still needs **no** network. The demo server is
  rate-limited/best-effort — production should point `baseUrl` at a self-hosted instance.
- The empty-lift **Resume** bug fixed earlier still holds — the centre shows ＋ Start here.

## Handed to the next slice
The route model, snapping, and saved-routes list are in place. The immediate next step is
**start-a-run-from-a-saved-route with off-route awareness**, reusing `RunMap` (planned underlay +
live trace), `Run.routeId`, and a new pure distance-from-route helper in `domain/run`.
