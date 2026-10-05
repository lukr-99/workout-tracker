package com.lukr99.workout.data.routing

/**
 * Snaps planner waypoints to roads/paths. The **only** class that talks to a routing provider, so the
 * provider (currently keyless OSRM) can be swapped without touching the planner UI. Live-run tracking
 * needs no routing — this is used only when planning/saving a [com.lukr99.workout.domain.run.Route].
 */
interface RoutingClient {
    /** Snap [waypoints] into a single walked/run route, or null if none could be found. */
    suspend fun snap(waypoints: List<LatLon>): SnappedRoute?
}
