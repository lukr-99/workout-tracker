package com.lukr99.workout.ui.run.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lukr99.workout.data.map.MapStyle
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point

/**
 * Provider-agnostic map surface for Run Mode. The **only** place screens touch a map SDK — every run
 * screen (live, detail, planner) renders through here, so the map provider (currently MapLibre) or
 * tile source ([MapStyle]) can be swapped without touching UI code.
 *
 * Renders the dark vector basemap, the user's location (blue dot, follows while granted), and the
 * **growing ember trace polyline** ([traceSegments], one line per pause-separated segment) for a
 * live or completed run.
 *
 * @param userLocationEnabled true once `ACCESS_FINE_LOCATION` is granted — gates the location layer.
 * @param recenterSignal increment to snap the camera back to the heading-follow mode + close zoom.
 * @param traceSegments the run trace split into continuous segments (`(lat, lon)` each); drawn as an
 *   ember line per segment so a manual pause breaks the line instead of joining across the gap.
 * @param traceColor ARGB colour for the trace line (the theme's ember by default).
 */
@Composable
fun RunMap(
    userLocationEnabled: Boolean,
    modifier: Modifier = Modifier,
    styleUrl: String = MapStyle.DARK_VECTOR_STYLE_URL,
    recenterSignal: Int = 0,
    compassSignal: Int = 0,
    headingFollow: Boolean = true,
    traceSegments: List<List<Pair<Double, Double>>> = emptyList(),
    traceColor: Int = DEFAULT_EMBER,
    fitTrace: Boolean = false,
    waypoints: List<Pair<Double, Double>> = emptyList(),
    plannedRoute: List<Pair<Double, Double>> = emptyList(),
    onMapTap: ((Double, Double) -> Unit)? = null,
    onBearingChanged: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Holds the async-created map/style so effects can act on them once ready.
    val holder = remember { MapHolder(traceColor, fitTrace) }
    holder.onTap = onMapTap
    holder.onBearing = onBearingChanged

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                holder.map = map
                // We draw our own compass button in the run controls (grouped with recenter/music),
                // so hide MapLibre's built-in overlay — it otherwise sits under the live stats panel.
                map.uiSettings.isCompassEnabled = false
                map.addOnCameraMoveListener {
                    holder.onBearing?.invoke(map.cameraPosition.bearing.toFloat())
                }
                map.addOnMapClickListener { latLng ->
                    holder.onTap?.invoke(latLng.latitude, latLng.longitude)
                    holder.onTap != null
                }
                map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                    holder.onStyleReady(style, context, userLocationEnabled)
                }
            }
        }
    }

    // Forward the host lifecycle to the MapView (MapLibre requires this for GL + location updates).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Turn the location layer on when permission is granted (idempotent; safe before the map loads).
    LaunchedEffect(userLocationEnabled) {
        if (userLocationEnabled) holder.enableLocation(context)
    }

    // Redraw the trace polyline as it grows (one line per continuous segment).
    LaunchedEffect(traceSegments) {
        holder.updateTrace(traceSegments)
    }

    // Redraw planner waypoint markers.
    LaunchedEffect(waypoints) {
        holder.updateWaypoints(waypoints)
    }

    // Redraw the faint planned-route underlay (start-a-run-from-route).
    LaunchedEffect(plannedRoute) {
        holder.updatePlanned(plannedRoute)
    }

    // Recenter (re-center on the runner + close zoom), respecting the current north-up/heading choice.
    LaunchedEffect(recenterSignal) {
        if (recenterSignal > 0) holder.applyFollow(headingFollow, zoom = true)
    }

    // Compass toggle: flip between north-up and locking the map to the phone's heading, no re-zoom.
    LaunchedEffect(compassSignal) {
        if (compassSignal > 0) holder.applyFollow(headingFollow, zoom = false)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Mutable async holder — the map and style arrive after `getMapAsync` / `setStyle` callbacks. */
private class MapHolder(private val traceColor: Int, private val fitTrace: Boolean) {
    var map: MapLibreMap? = null
    var style: Style? = null
    var onTap: ((Double, Double) -> Unit)? = null
    var onBearing: ((Float) -> Unit)? = null
    private var locationActive = false
    private var pendingTrace: List<List<Pair<Double, Double>>> = emptyList()
    private var pendingWaypoints: List<Pair<Double, Double>> = emptyList()
    private var pendingPlanned: List<Pair<Double, Double>> = emptyList()

    fun onStyleReady(style: Style, context: android.content.Context, enableLocation: Boolean) {
        this.style = style
        // Planned-route underlay first, so the live trace + waypoints draw over it.
        style.addSource(GeoJsonSource(PLANNED_SOURCE))
        style.addLayer(
            LineLayer(PLANNED_LAYER, PLANNED_SOURCE).withProperties(
                PropertyFactory.lineColor(traceColor),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineOpacity(0.35f),
                PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addSource(GeoJsonSource(TRACE_SOURCE))
        style.addLayer(
            LineLayer(TRACE_LAYER, TRACE_SOURCE).withProperties(
                PropertyFactory.lineColor(traceColor),
                PropertyFactory.lineWidth(5.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addSource(GeoJsonSource(WAYPOINT_SOURCE))
        style.addLayer(
            CircleLayer(WAYPOINT_LAYER, WAYPOINT_SOURCE).withProperties(
                PropertyFactory.circleColor(traceColor),
                PropertyFactory.circleRadius(6f),
                PropertyFactory.circleStrokeColor(-0x1), // white
                PropertyFactory.circleStrokeWidth(2f),
            ),
        )
        if (pendingPlanned.isNotEmpty()) updatePlanned(pendingPlanned)
        if (pendingTrace.isNotEmpty()) updateTrace(pendingTrace)
        if (pendingWaypoints.isNotEmpty()) updateWaypoints(pendingWaypoints)
        if (enableLocation) enableLocation(context)
    }

    fun updatePlanned(points: List<Pair<Double, Double>>) {
        pendingPlanned = points
        val source = style?.getSourceAs<GeoJsonSource>(PLANNED_SOURCE) ?: return
        if (points.size < 2) {
            source.setGeoJson(FeatureCollectionEmpty)
            return
        }
        val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.second, it.first) })
        source.setGeoJson(Feature.fromGeometry(line))
    }

    fun updateWaypoints(points: List<Pair<Double, Double>>) {
        pendingWaypoints = points
        val source = style?.getSourceAs<GeoJsonSource>(WAYPOINT_SOURCE) ?: return
        source.setGeoJson(
            FeatureCollection.fromFeatures(
                points.map { Feature.fromGeometry(Point.fromLngLat(it.second, it.first)) },
            ),
        )
    }

    @SuppressLint("MissingPermission")
    fun enableLocation(context: android.content.Context) {
        val map = map ?: return
        val style = style ?: return
        if (!style.isFullyLoaded) return
        val component = map.locationComponent
        if (!locationActive) {
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .locationEngine(LocationEngineDefault.getDefaultLocationEngine(context))
                    .build(),
            )
            locationActive = true
        }
        component.isLocationComponentEnabled = true
        // Follow the runner *and* rotate the map to the direction they're facing (heading up), so the
        // road ahead is always at the top — the "follow where I'm facing" ask. COMPASS render keeps the
        // location puck's heading arrow. A pinch/rotate gesture disengages this; recenter re-arms it.
        component.cameraMode = CameraMode.TRACKING_COMPASS
        component.renderMode = RenderMode.COMPASS
        component.lastKnownLocation?.let {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), MapStyle.FOLLOW_ZOOM),
            )
        }
    }

    fun updateTrace(segments: List<List<Pair<Double, Double>>>) {
        pendingTrace = segments
        val source = style?.getSourceAs<GeoJsonSource>(TRACE_SOURCE) ?: return
        // One LineString per continuous segment; a manual pause splits the trace so the paused-and-
        // walked stretch reads as a gap rather than a straight line joining the two ends.
        val lines = segments
            .filter { it.size >= 2 }
            .map { seg -> LineString.fromLngLats(seg.map { Point.fromLngLat(it.second, it.first) }) }
        if (lines.isEmpty()) {
            source.setGeoJson(FeatureCollectionEmpty)
            return
        }
        source.setGeoJson(Feature.fromGeometry(MultiLineString.fromLineStrings(lines)))
        if (fitTrace) fitCameraTo(segments.flatten())
    }

    /** Frame the whole trace (run detail): fit the camera to the polyline's bounds with padding. */
    private fun fitCameraTo(points: List<Pair<Double, Double>>) {
        val map = map ?: return
        val builder = LatLngBounds.Builder()
        points.forEach { builder.include(LatLng(it.first, it.second)) }
        runCatching {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), FIT_PADDING_PX))
        }
    }

    /**
     * Set how the camera follows the runner. [follow] `true` locks the map to the phone's heading
     * (rotates so the road ahead is up); `false` keeps it north-up. [zoom] `true` also snaps to the
     * close follow zoom (used by the recenter button; the compass toggle leaves zoom alone).
     */
    fun applyFollow(follow: Boolean, zoom: Boolean) {
        val map = map ?: return
        val component = map.locationComponent
        if (!locationActive) return
        if (follow) {
            component.cameraMode = CameraMode.TRACKING_COMPASS
        } else {
            component.cameraMode = CameraMode.TRACKING
            map.animateCamera(CameraUpdateFactory.bearingTo(0.0))
        }
        if (zoom) component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
    }

    companion object {
        private const val PLANNED_SOURCE = "run-planned-src"
        private const val PLANNED_LAYER = "run-planned-layer"
        private const val TRACE_SOURCE = "run-trace-src"
        private const val TRACE_LAYER = "run-trace-layer"
        private const val WAYPOINT_SOURCE = "run-waypoint-src"
        private const val WAYPOINT_LAYER = "run-waypoint-layer"
        private const val FIT_PADDING_PX = 90
        private const val FeatureCollectionEmpty = "{\"type\":\"FeatureCollection\",\"features\":[]}"
    }
}

private const val DEFAULT_EMBER = 0xFFE8622C.toInt()
