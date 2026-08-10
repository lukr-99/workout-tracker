package com.lukr99.workout.data.map

/**
 * Map basemap configuration — the one place the tile/style source is chosen, so screens stay
 * provider-agnostic (they only ever touch `ui/run/components/MapView.kt`).
 *
 * **R0 dev:** a keyless dark **vector** style served by OpenFreeMap, whose tiles are Protomaps
 * **PMTiles** under the hood — matching the locked "MapLibre + Protomaps, keyless" decision and our
 * near-black aesthetic, with nothing to sign or key. It fetches over the network at runtime.
 *
 * **Later (R5 — offline):** swap [DARK_VECTOR_STYLE_URL] for a bundled/self-hosted `.pmtiles`
 * region file read through MapLibre's PMTiles source. Because every screen goes through `MapView`,
 * that swap is a one-line change here. Keep any hosted key out of the repo (git-ignored properties).
 */
object MapStyle {

    /** Keyless dark vector style (Protomaps/PMTiles-backed via OpenFreeMap). */
    const val DARK_VECTOR_STYLE_URL: String = "https://tiles.openfreemap.org/styles/dark"

    /**
     * Camera zoom while following the runner. Deliberately close (street-level) so the road and the
     * ember trace right around you are legible mid-run — the common ask is "let me see the path I'm
     * actually on", not the whole city. Pinch still overrides it; the recenter button restores it.
     */
    const val FOLLOW_ZOOM: Double = 16.8
}
