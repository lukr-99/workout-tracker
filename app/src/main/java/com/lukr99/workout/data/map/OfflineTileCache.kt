package com.lukr99.workout.data.map

import android.content.Context
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

/**
 * Downloads the map region around a route/recent trace for **offline** use — the deferred R3 slice,
 * closed in R5. All MapLibre `OfflineManager` specifics live here so screens stay provider-agnostic
 * (they still only touch [com.lukr99.workout.ui.run.components.RunMap]); a different tile provider
 * would swap this file alone. Live-run tracking already needs no network — this just makes the *map*
 * usable without one for a known area.
 *
 * A region is a tile pyramid over a [LatLngBounds] between [MIN_ZOOM] and [MAX_ZOOM] for the same
 * [MapStyle.DARK_VECTOR_STYLE_URL] the app renders, so cached tiles serve the live/detail maps too.
 */
class OfflineTileCache(context: Context) {

    private val appContext = context.applicationContext
    private val manager: OfflineManager by lazy {
        MapLibre.getInstance(appContext)
        OfflineManager.getInstance(appContext)
    }

    /** Progress of an in-flight download, 0..100, plus terminal complete/error signals. */
    interface Listener {
        fun onProgress(percent: Int)
        fun onComplete()
        fun onError(reason: String)
    }

    /**
     * Cache tiles covering [bounds] (with a little padding) under [name]. Fires [listener] callbacks
     * on the main thread. Best-effort: any MapLibre failure is reported through [Listener.onError],
     * never thrown, so the UI can show a gentle failure without crashing.
     */
    fun download(bounds: LatLngBounds, name: String, listener: Listener) {
        val definition = OfflineTilePyramidRegionDefinition(
            MapStyle.DARK_VECTOR_STYLE_URL,
            bounds,
            MIN_ZOOM,
            MAX_ZOOM,
            appContext.resources.displayMetrics.density,
        )
        val metadata = "{\"$METADATA_NAME\":${quote(name)}}".toByteArray(Charsets.UTF_8)

        manager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(region: OfflineRegion) {
                    region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            val required = status.requiredResourceCount.coerceAtLeast(1)
                            val percent = (status.completedResourceCount * 100 / required)
                                .coerceIn(0, 100).toInt()
                            if (status.isComplete) {
                                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                listener.onComplete()
                            } else {
                                listener.onProgress(percent)
                            }
                        }

                        override fun onError(error: OfflineRegionError) {
                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            listener.onError(error.reason)
                        }

                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            listener.onError("Tile limit ($limit) exceeded")
                        }
                    })
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) = listener.onError(error)
            },
        )
    }

    /** Delete every cached offline region (used to reclaim space). Best-effort. */
    fun clearAll(onDone: () -> Unit = {}) {
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val list = offlineRegions?.toList().orEmpty()
                if (list.isEmpty()) { onDone(); return }
                var remaining = list.size
                list.forEach { region ->
                    region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                        override fun onDelete() { if (--remaining == 0) onDone() }
                        override fun onError(error: String) { if (--remaining == 0) onDone() }
                    })
                }
            }

            override fun onError(error: String) = onDone()
        })
    }

    private fun quote(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    companion object {
        const val MIN_ZOOM: Double = 10.0
        const val MAX_ZOOM: Double = 16.0
        private const val METADATA_NAME = "name"

        /** A padded bounding box around a `(lat, lon)` trace/route; null if there are no points. */
        fun boundsForPath(points: List<Pair<Double, Double>>): LatLngBounds? {
            if (points.isEmpty()) return null
            var minLat = Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
            var minLon = Double.MAX_VALUE; var maxLon = -Double.MAX_VALUE
            for ((lat, lon) in points) {
                minLat = minOf(minLat, lat); maxLat = maxOf(maxLat, lat)
                minLon = minOf(minLon, lon); maxLon = maxOf(maxLon, lon)
            }
            val padLat = ((maxLat - minLat) * 0.15).coerceAtLeast(0.003)
            val padLon = ((maxLon - minLon) * 0.15).coerceAtLeast(0.003)
            return LatLngBounds.Builder()
                .include(LatLng(maxLat + padLat, maxLon + padLon))
                .include(LatLng(minLat - padLat, minLon - padLon))
                .build()
        }
    }
}
