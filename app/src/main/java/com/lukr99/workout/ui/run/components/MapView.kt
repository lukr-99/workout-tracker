package com.lukr99.workout.ui.run.components

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
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

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

private const val DEFAULT_EMBER = 0xFFE8622C.toInt()
