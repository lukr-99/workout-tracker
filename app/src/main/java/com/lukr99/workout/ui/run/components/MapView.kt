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
import org.maplibre.geojson.Point

/**
 * Provider-agnostic map surface for Run Mode. The **only** place screens touch a map SDK — every run
 * screen (live, detail, planner) renders through here, so the map provider (currently MapLibre) or
 * tile source ([MapStyle]) can be swapped without touching UI code.
 *
 * Renders the dark vector basemap, the user's location (blue dot, follows while granted), and the
 * **growing ember trace polyline** ([tracePoints]) for a live or completed run.
 *
 * @param userLocationEnabled true once `ACCESS_FINE_LOCATION` is granted — gates the location layer.
 * @param recenterSignal increment to snap the camera back to a location-tracking follow mode.
 * @param tracePoints ordered `(lat, lon)` of the run trace; drawn as an ember line that grows live.
 * @param traceColor ARGB colour for the trace line (the theme's ember by default).
 */
@Composable
fun RunMap(
    userLocationEnabled: Boolean,
    modifier: Modifier = Modifier,
    styleUrl: String = MapStyle.DARK_VECTOR_STYLE_URL,
    recenterSignal: Int = 0,
    tracePoints: List<Pair<Double, Double>> = emptyList(),
    traceColor: Int = DEFAULT_EMBER,
    fitTrace: Boolean = false,
    waypoints: List<Pair<Double, Double>> = emptyList(),
    plannedRoute: List<Pair<Double, Double>> = emptyList(),
    onMapTap: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Holds the async-created map/style so effects can act on them once ready.
    val holder = remember { MapHolder(traceColor, fitTrace) }
    holder.onTap = onMapTap

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                holder.map = map
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

    // Redraw the trace polyline as it grows.
    LaunchedEffect(tracePoints) {
        holder.updateTrace(tracePoints)
    }

    // Redraw planner waypoint markers.
    LaunchedEffect(waypoints) {
        holder.updateWaypoints(waypoints)
    }

    // Redraw the faint planned-route underlay (start-a-run-from-route).
    LaunchedEffect(plannedRoute) {
        holder.updatePlanned(plannedRoute)
    }

    // Recenter/follow when asked.
    LaunchedEffect(recenterSignal) {
        if (recenterSignal > 0) holder.recenter()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Mutable async holder — the map and style arrive after `getMapAsync` / `setStyle` callbacks. */
private class MapHolder(private val traceColor: Int, private val fitTrace: Boolean) {
    var map: MapLibreMap? = null
    var style: Style? = null
    var onTap: ((Double, Double) -> Unit)? = null
    private var locationActive = false
    private var pendingTrace: List<Pair<Double, Double>> = emptyList()
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
        component.cameraMode = CameraMode.TRACKING
        component.renderMode = RenderMode.COMPASS
        component.lastKnownLocation?.let {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), MapStyle.FOLLOW_ZOOM),
            )
        }
    }

    fun updateTrace(points: List<Pair<Double, Double>>) {
        pendingTrace = points
        val source = style?.getSourceAs<GeoJsonSource>(TRACE_SOURCE) ?: return
        if (points.size < 2) {
            source.setGeoJson(FeatureCollectionEmpty)
            return
        }
        val line = LineString.fromLngLats(points.map { Point.fromLngLat(it.second, it.first) })
        source.setGeoJson(Feature.fromGeometry(line))
        if (fitTrace) fitCameraTo(points)
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

    fun recenter() {
        val component = map?.locationComponent ?: return
        if (!locationActive) return
        component.cameraMode = CameraMode.TRACKING
        component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
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
